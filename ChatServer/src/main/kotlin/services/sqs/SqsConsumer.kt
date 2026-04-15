package org.chatserver.services.sqs

import io.ktor.websocket.Frame
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.ChatMessage
import org.chatserver.models.PresenceEvent
import org.chatserver.models.SqsEnvelope
import org.chatserver.session.SessionStore
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient

class SqsConsumer(
    private val sqsClient: SqsClient,
    private val sessionStore: SessionStore,
    private val pendingMessageRepository: PendingMessageRepository,
) {
    companion object {
        private const val MAX_MESSAGES = 10
        private const val WAIT_TIME_SECONDS = 20
    }

    private val logger = LoggerFactory.getLogger(SqsConsumer::class.java)

    suspend fun start(queueUrl: String) {
        logger.info("SQS consumer started on $queueUrl")
        while (true) {
            val response =
                sqsClient.receiveMessage {
                    it.queueUrl(queueUrl)
                    it.maxNumberOfMessages(MAX_MESSAGES)
                    it.waitTimeSeconds(WAIT_TIME_SECONDS)
                }

            for (message in response.messages()) {
                val envelope = Json.decodeFromString<SqsEnvelope>(message.body())
                when (envelope.type) {
                    SqsEnvelope.CHAT -> handleChat(Json.decodeFromString(envelope.payload))
                    SqsEnvelope.PRESENCE -> handlePresence(Json.decodeFromString(envelope.payload))
                    else -> logger.warn("Unknown SQS message type: ${envelope.type}")
                }

                sqsClient.deleteMessage {
                    it.queueUrl(queueUrl)
                    it.receiptHandle(message.receiptHandle())
                }
            }
        }
    }

    private suspend fun handleChat(chatMessage: ChatMessage) {
        val session = sessionStore.get(chatMessage.toUserId)
        if (session != null) {
            session.send(Frame.Text(Json.encodeToString(chatMessage)))
            logger.info("Delivered message to user ${chatMessage.toUserId}")
        } else {
            logger.info("User ${chatMessage.toUserId} no longer connected, saving to pending")
            pendingMessageRepository.save(chatMessage)
        }
    }

    private suspend fun handlePresence(event: PresenceEvent) {
        val frame = Frame.Text(Json.encodeToString(event))
        sessionStore.getAll().forEach { userId ->
            sessionStore.get(userId)?.send(frame)
        }
        logger.debug("Broadcast presence for ${event.userId} online=${event.online} to ${sessionStore.getAll().size} session(s)")
    }
}
