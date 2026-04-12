package org.chatserver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.chatserver.data.repository.MessageRepository
import org.chatserver.models.HealthResponse
import org.chatserver.models.HeartbeatRequest
import org.chatserver.services.presence.PresenceClient
import org.koin.ktor.ext.inject

fun Application.configureRouting(internalApiKey: String = System.getenv("INTERNAL_API_KEY") ?: error("INTERNAL_API_KEY env var not set")) {
    val messageRepository by inject<MessageRepository>()
    val presenceClient by inject<PresenceClient>()

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse("ok"))
        }

        get("/conversations/{conversationId}/messages") {
            if (call.request.headers["x-internal-key"] != internalApiKey) {
                return@get call.respond(HttpStatusCode.Forbidden)
            }
            val conversationId = call.parameters["conversationId"] ?: return@get
            call.respond(messageRepository.getMessages(conversationId))
        }

        post("/presence/batch") {
            if (call.request.headers["x-internal-key"] != internalApiKey) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }
            val body = call.receive<HeartbeatRequest>()
            val presence = presenceClient.batchPresence(body.userIds)
            call.respond(mapOf("presence" to presence))
        }
    }
}
