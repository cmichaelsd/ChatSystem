package org.chatserver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.chatserver.data.repository.MessageRepository
import org.chatserver.models.HealthResponse
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val messageRepository by inject<MessageRepository>()

    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, HealthResponse("ok"))
        }

        get("/conversations/{conversationId}/messages") {
            val conversationId = call.parameters["conversationId"] ?: return@get
            call.respond(messageRepository.getMessages(conversationId))
        }
    }
}
