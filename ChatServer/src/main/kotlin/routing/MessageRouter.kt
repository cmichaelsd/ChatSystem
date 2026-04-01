package org.chatserver.routing

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.model.ChatMessage
import org.chatserver.registry.ConversationRegistry
import org.chatserver.registry.ServerRegistry
import org.chatserver.registry.UserRegistry
import org.chatserver.repository.MessageRepository
import org.chatserver.repository.PendingMessageRepository
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

        val recipients = members.filter { it != fromUserId }
        messageRepository.save(fromUserId, conversationId, content)

        for (recipientId in recipients) {
            deliver(ChatMessage(fromUserId, recipientId, conversationId, content))
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

        sqsClient.sendMessage {
            it.queueUrl(queueUrl)
            it.messageBody(Json.encodeToString(message))
        }

        logger.info("Routed message from ${message.fromUserId} to ${message.toUserId} via $targetServerId")
    }
}
