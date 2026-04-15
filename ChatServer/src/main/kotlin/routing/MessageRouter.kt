package org.chatserver.routing

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.data.registry.ServerRegistry
import org.chatserver.data.registry.UserRegistry
import org.chatserver.data.repository.MessageRepository
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.ChatMessage
import org.chatserver.models.SqsEnvelope
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient

class MessageRouter(
    private val sqsClient: SqsClient,
    private val userRegistry: UserRegistry,
    private val serverRegistry: ServerRegistry,
    private val pendingMessageRepository: PendingMessageRepository,
    private val conversationRegistry: ConversationRegistry,
    private val messageRepository: MessageRepository,
) {
    private val logger = LoggerFactory.getLogger(MessageRouter::class.java)

    fun route(
        fromUserId: String,
        conversationId: String,
        content: String,
    ) {
        val members = conversationRegistry.getMembers(conversationId)

        if (members.isEmpty()) {
            logger.warn("No members found for conversation $conversationId")
            return
        }

        messageRepository.save(fromUserId, conversationId, content)

        for (recipientId in members) {
            deliver(ChatMessage(fromUserId = fromUserId, toUserId = recipientId, conversationId = conversationId, content = content))
        }
    }

    private fun deliver(message: ChatMessage) {
        val targetServerId =
            userRegistry.getServerId(message.toUserId) ?: run {
                logger.info("User ${message.toUserId} is offline, saving to pending")
                pendingMessageRepository.save(message)
                return
            }

        val queueUrl =
            serverRegistry.getQueueUrl(targetServerId) ?: run {
                logger.warn("Server $targetServerId has no queue URL — likely dead, cleaning up stale entry for user ${message.toUserId}")
                userRegistry.deregister(message.toUserId)
                pendingMessageRepository.save(message)
                return
            }

        val envelope = SqsEnvelope(type = SqsEnvelope.CHAT, payload = Json.encodeToString(message))
        sqsClient.sendMessage {
            it.queueUrl(queueUrl)
            it.messageBody(Json.encodeToString(envelope))
        }

        logger.info("Routed message from ${message.fromUserId} to ${message.toUserId} via $targetServerId")
    }
}
