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
import org.chatserver.data.registry.UserRegistry
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.InboundMessage
import org.chatserver.routing.MessageRouter
import org.chatserver.session.SessionStore
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Sockets")

fun Application.configureSockets() {
    install(WebSockets)

    val userRegistry by inject<UserRegistry>()
    val sessionStore by inject<SessionStore>()
    val messageRouter by inject<MessageRouter>()
    val pendingMessageRepository by inject<PendingMessageRepository>()

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
                    logger.info("User $userId disconnected")
                }
            }
        }
    }
}
