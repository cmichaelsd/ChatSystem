package org.chatserver.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.chatserver.repository.MessageRepository
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val messageRepository by inject<MessageRepository>()

    routing {
        get("/conversations/{conversationId}/messages") {
            val conversationId = call.parameters["conversationId"] ?: return@get
            call.respond(messageRepository.getMessages(conversationId))
        }
    }
}
