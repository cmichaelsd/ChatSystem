package org.chatserver.plugins

import com.amazonaws.xray.AWSXRay
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.data.registry.ServerRegistry
import org.chatserver.data.registry.UserRegistry
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.InboundMessage
import org.chatserver.models.PresenceEvent
import org.chatserver.models.SqsEnvelope
import org.chatserver.routing.MessageRouter
import org.chatserver.session.SessionStore
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient

private val logger = LoggerFactory.getLogger("Sockets")

fun Application.configureSockets() {
    install(WebSockets)

    val koin = getKoin()
    val userRegistry = koin.get<UserRegistry>()
    val conversationRegistry = koin.get<ConversationRegistry>()
    val sessionStore = koin.get<SessionStore>()
    val messageRouter = koin.get<MessageRouter>()
    val pendingMessageRepository = koin.get<PendingMessageRepository>()
    val serverRegistry = koin.get<ServerRegistry>()
    val sqsClient = koin.get<SqsClient>()

    routing {
        authenticate("auth-jwt") {
            webSocket("/ws") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject
                if (userId == null) {
                    close()
                    return@webSocket
                }

                logger.info("User $userId connected")
                AWSXRay.beginSegment("ws-connect $userId")
                userRegistry.register(userId)
                sessionStore.add(userId, this)
                AWSXRay.endSegment()

                try {
                    val groupmates = conversationRegistry.getGroupmates(userId)
                    val onlineGroupmates = userRegistry.getConnectedUsersFrom(groupmates)
                    logger.info("Presence snapshot for $userId: ${groupmates.size} groupmate(s), ${onlineGroupmates.size} online")
                    send(Frame.Text(Json.encodeToString(PresenceEvent(userId = userId, online = true))))
                    for (onlineUserId in onlineGroupmates) {
                        send(Frame.Text(Json.encodeToString(PresenceEvent(userId = onlineUserId, online = true))))
                    }

                    broadcastPresence(userId, online = true, serverRegistry, sqsClient)

                    val pending = pendingMessageRepository.fetchAndClear(userId)
                    if (pending.isNotEmpty()) {
                        logger.info("Delivering ${pending.size} pending message(s) to $userId")
                        pending.forEach { send(Frame.Text(Json.encodeToString(it))) }
                    }

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            try {
                                val inbound = Json.decodeFromString<InboundMessage>(frame.readText())
                                messageRouter.route(userId, inbound.conversationId, inbound.content)
                            } catch (e: Exception) {
                                logger.warn("Invalid message from $userId: ${e.message}")
                            }
                        }
                    }
                } finally {
                    AWSXRay.beginSegment("ws-disconnect $userId")
                    sessionStore.remove(userId)
                    val deregistered = userRegistry.deregister(userId)
                    if (deregistered) {
                        broadcastPresence(userId, online = false, serverRegistry, sqsClient)
                    }
                    AWSXRay.endSegment()
                    logger.info("User $userId disconnected")
                }
            }
        }
    }
}

private fun broadcastPresence(
    userId: String,
    online: Boolean,
    serverRegistry: ServerRegistry,
    sqsClient: SqsClient,
) {
    val envelope =
        Json.encodeToString(
            SqsEnvelope(
                type = SqsEnvelope.PRESENCE,
                payload = Json.encodeToString(PresenceEvent(userId = userId, online = online)),
            ),
        )
    for (queueUrl in serverRegistry.getAllQueueUrls()) {
        try {
            sqsClient.sendMessage {
                it.queueUrl(queueUrl)
                it.messageBody(envelope)
            }
        } catch (e: Exception) {
            logger.warn("Failed to broadcast presence to $queueUrl: ${e.message}")
        }
    }
}
