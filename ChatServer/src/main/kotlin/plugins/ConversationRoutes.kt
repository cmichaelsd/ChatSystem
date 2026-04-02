package org.chatserver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.chatserver.registry.ConversationRegistry
import org.koin.ktor.ext.inject

@Serializable
data class CreateConversationRequest(
    val conversationId: String,
    val memberIds: List<String>,
)

fun Application.configureConversationRoutes() {
    val conversationRegistry by inject<ConversationRegistry>()

    routing {
        post("/conversations") {
            val request = call.receive<CreateConversationRequest>()
            request.memberIds.forEach { conversationRegistry.addMember(request.conversationId, it) }
            call.respond(HttpStatusCode.Created)
        }

        post("/conversations/{conversationId}/members/{userId}") {
            val conversationId = call.parameters["conversationId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            conversationRegistry.addMember(conversationId, userId)
            call.respond(HttpStatusCode.Created)
        }

        delete("/conversations/{conversationId}/members/{userId}") {
            val conversationId = call.parameters["conversationId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.parameters["userId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            conversationRegistry.removeMember(conversationId, userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
