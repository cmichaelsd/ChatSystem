package org.chatserver.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.data.registry.ServerRegistry
import org.chatserver.models.CreateConversationRequest
import org.chatserver.models.GroupAddedPayload
import org.chatserver.models.SqsEnvelope
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient

private val logger = LoggerFactory.getLogger("ConversationRoutes")

fun Application.configureConversationRoutes(
    internalApiKey: String = System.getenv("INTERNAL_API_KEY") ?: error("INTERNAL_API_KEY env var not set"),
) {
    val conversationRegistry by inject<ConversationRegistry>()
    val serverRegistry by inject<ServerRegistry>()
    val sqsClient by inject<SqsClient>()

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
            broadcastGroupAdded(conversationId, userId, serverRegistry, sqsClient)
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

private fun broadcastGroupAdded(
    conversationId: String,
    userId: String,
    serverRegistry: ServerRegistry,
    sqsClient: SqsClient,
) {
    val envelope =
        Json.encodeToString(
            SqsEnvelope(
                type = SqsEnvelope.GROUP_ADDED,
                payload = Json.encodeToString(GroupAddedPayload(conversationId = conversationId, userId = userId)),
            ),
        )
    for (queueUrl in serverRegistry.getAllQueueUrls()) {
        try {
            sqsClient.sendMessage {
                it.queueUrl(queueUrl)
                it.messageBody(envelope)
            }
        } catch (e: Exception) {
            logger.warn("Failed to broadcast GROUP_ADDED to $queueUrl: ${e.message}")
        }
    }
}
