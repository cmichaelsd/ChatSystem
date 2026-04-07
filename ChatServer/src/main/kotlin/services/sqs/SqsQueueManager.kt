package org.chatserver.services.sqs

import org.chatserver.data.registry.ServerRegistry
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient

class SqsQueueManager(
    private val sqsClient: SqsClient,
    private val serverRegistry: ServerRegistry,
    private val serverId: String,
) {
    companion object {
        private const val QUEUE_PREFIX = "chat-server-"
    }

    private val logger = LoggerFactory.getLogger(SqsQueueManager::class.java)

    fun init(): String {
        val queueName = "$QUEUE_PREFIX$serverId"
        val response = sqsClient.createQueue { it.queueName(queueName) }
        val queueUrl = response.queueUrl()
        serverRegistry.updateQueueUrl(queueUrl)
        logger.info("Claimed SQS queue: $queueUrl")
        return queueUrl
    }
}
