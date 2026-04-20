package org.chatserver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.models.CreateConversationRequest
import org.koin.ktor.ext.inject

fun Application.configureConversationRoutes(
    internalApiKey: String = System.getenv("INTERNAL_API_KEY") ?: error("INTERNAL_API_KEY env var not set"),
) {
    val conversationRegistry by inject<ConversationRegistry>()

    routing {
        post("/conversations") {
            if (call.request.headers["x-internal-key"] != internalApiKey) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }
            val request = call.receive<CreateConversationRequest>()
            request.memberIds.forEach { conversationRegistry.addMember(request.conversationId, it) }
            call.respond(HttpStatusCode.Created)
        }

        post("/conversations/{conversationId}/members/{userId}") {
            if (call.request.headers["x-internal-key"] != internalApiKey) {
                return@post call.respond(HttpStatusCode.Forbidden)
            }
            val conversationId = call.parameters["conversationId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            conversationRegistry.addMember(conversationId, userId)
            call.respond(HttpStatusCode.Created)
        }

        delete("/conversations/{conversationId}/members/{userId}") {
            if (call.request.headers["x-internal-key"] != internalApiKey) {
                return@delete call.respond(HttpStatusCode.Forbidden)
            }
            val conversationId = call.parameters["conversationId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.parameters["userId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            conversationRegistry.removeMember(conversationId, userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
