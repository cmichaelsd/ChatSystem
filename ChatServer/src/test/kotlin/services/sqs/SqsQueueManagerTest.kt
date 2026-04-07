package org.chatserver.services.sqs

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.chatserver.data.registry.ServerRegistry
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals

class SqsQueueManagerTest {
    private val sqsClient = mockk<SqsClient>()
    private val serverRegistry = mockk<ServerRegistry>(relaxed = true)

    @Test
    fun `init creates queue named after server ID`() {
        val serverId = "server-abc"
        val manager = SqsQueueManager(sqsClient, serverRegistry, serverId)
        every { sqsClient.createQueue(any<Consumer<CreateQueueRequest.Builder>>()) } returns
            CreateQueueResponse.builder().queueUrl("https://sqs/chat-server-$serverId").build()

        manager.init()

        verify { sqsClient.createQueue(any<Consumer<CreateQueueRequest.Builder>>()) }
    }

    @Test
    fun `init registers the queue URL with ServerRegistry`() {
        val serverId = "server-xyz"
        val queueUrl = "https://sqs/chat-server-$serverId"
        val manager = SqsQueueManager(sqsClient, serverRegistry, serverId)
        every { sqsClient.createQueue(any<Consumer<CreateQueueRequest.Builder>>()) } returns
            CreateQueueResponse.builder().queueUrl(queueUrl).build()

        manager.init()

        verify { serverRegistry.updateQueueUrl(queueUrl) }
    }

    @Test
    fun `init returns the queue URL`() {
        val serverId = "server-123"
        val queueUrl = "https://sqs/chat-server-$serverId"
        val manager = SqsQueueManager(sqsClient, serverRegistry, serverId)
        every { sqsClient.createQueue(any<Consumer<CreateQueueRequest.Builder>>()) } returns
            CreateQueueResponse.builder().queueUrl(queueUrl).build()

        val result = manager.init()

        assertEquals(queueUrl, result)
    }
}
