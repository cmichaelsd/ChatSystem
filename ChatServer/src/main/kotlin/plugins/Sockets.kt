package org.chatserver.plugins

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
import org.chatserver.data.registry.ServerRegistry
import org.chatserver.data.registry.UserRegistry
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.InboundMessage
import org.chatserver.models.PresenceEvent
import org.chatserver.models.SqsEnvelope
import org.chatserver.routing.MessageRouter
import org.chatserver.session.SessionStore
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient

private val logger = LoggerFactory.getLogger("Sockets")

fun Application.configureSockets() {
    install(WebSockets)

    val userRegistry by inject<UserRegistry>()
    val sessionStore by inject<SessionStore>()
    val messageRouter by inject<MessageRouter>()
    val pendingMessageRepository by inject<PendingMessageRepository>()
    val serverRegistry by inject<ServerRegistry>()
    val sqsClient by inject<SqsClient>()

    routing {
        authenticate("auth-jwt") {
            webSocket("/ws") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject
                if (userId == null) {
                    close()
                    return@webSocket
                }

                logger.info("User $userId connected")
                userRegistry.register(userId)
                sessionStore.add(userId, this)

                // Send a cross-instance presence snapshot by reading UserConnections,
                // which reflects all connected users regardless of which server they're on.
                val alreadyOnline = userRegistry.getAllConnectedUserIds()
                for (onlineUserId in alreadyOnline) {
                    send(Frame.Text(Json.encodeToString(PresenceEvent(userId = onlineUserId, online = true))))
                }

                // Notify all other servers that this user came online.
                broadcastPresence(userId, online = true, serverRegistry, sqsClient)

                val pending = pendingMessageRepository.fetchAndClear(userId)
                if (pending.isNotEmpty()) {
                    logger.info("Delivering ${pending.size} pending message(s) to $userId")
                    pending.forEach { send(Frame.Text(Json.encodeToString(it))) }
                }

                try {
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
                    sessionStore.remove(userId)
                    userRegistry.deregister(userId)
                    broadcastPresence(userId, online = false, serverRegistry, sqsClient)
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
