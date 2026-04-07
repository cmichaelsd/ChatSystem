package org.chatserver.routing

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.data.registry.ServerRegistry
import org.chatserver.data.registry.UserRegistry
import org.chatserver.data.repository.MessageRepository
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.ChatMessage
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.util.function.Consumer
import kotlin.test.Test

class MessageRouterTest {
    private val sqsClient = mockk<SqsClient>(relaxed = true)
    private val userRegistry = mockk<UserRegistry>(relaxed = true)
    private val serverRegistry = mockk<ServerRegistry>()
    private val pendingMessageRepository = mockk<PendingMessageRepository>(relaxed = true)
    private val conversationRegistry = mockk<ConversationRegistry>()
    private val messageRepository = mockk<MessageRepository>(relaxed = true)

    private val router =
        MessageRouter(
            sqsClient,
            userRegistry,
            serverRegistry,
            pendingMessageRepository,
            conversationRegistry,
            messageRepository,
        )

    @Test
    fun `route saves message to repository before delivering`() {
        every { conversationRegistry.getMembers("conv-1") } returns listOf("alice", "bob")
        every { userRegistry.getServerId("bob") } returns "server-2"
        every { serverRegistry.getQueueUrl("server-2") } returns "https://sqs/queue-2"

        router.route("alice", "conv-1", "hello")

        verify { messageRepository.save("alice", "conv-1", "hello") }
    }

    @Test
    fun `route sends message to SQS when recipient is online on another server`() {
        every { conversationRegistry.getMembers("conv-1") } returns listOf("alice", "bob")
        every { userRegistry.getServerId("bob") } returns "server-2"
        every { serverRegistry.getQueueUrl("server-2") } returns "https://sqs/queue-2"

        router.route("alice", "conv-1", "hello")

        verify { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) }
        verify(exactly = 0) { pendingMessageRepository.save(any()) }
    }

    @Test
    fun `route saves to pending when recipient is offline`() {
        every { conversationRegistry.getMembers("conv-1") } returns listOf("alice", "bob")
        every { userRegistry.getServerId("bob") } returns null

        router.route("alice", "conv-1", "hello")

        verify { pendingMessageRepository.save(ChatMessage("alice", "bob", "conv-1", "hello")) }
        verify(exactly = 0) { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) }
    }

    @Test
    fun `route deregisters stale user and saves to pending when server has no queue URL`() {
        every { conversationRegistry.getMembers("conv-1") } returns listOf("alice", "bob")
        every { userRegistry.getServerId("bob") } returns "dead-server"
        every { serverRegistry.getQueueUrl("dead-server") } returns null

        router.route("alice", "conv-1", "hello")

        verify { userRegistry.deregister("bob") }
        verify { pendingMessageRepository.save(ChatMessage("alice", "bob", "conv-1", "hello")) }
        verify(exactly = 0) { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) }
    }

    @Test
    fun `route skips delivery entirely when conversation has no members`() {
        every { conversationRegistry.getMembers("empty-conv") } returns emptyList()

        router.route("alice", "empty-conv", "hello")

        verify(exactly = 0) { messageRepository.save(any(), any(), any()) }
        verify(exactly = 0) { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) }
    }

    @Test
    fun `route does not deliver message to the sender`() {
        every { conversationRegistry.getMembers("conv-1") } returns listOf("alice")

        router.route("alice", "conv-1", "hello")

        // alice is both sender and the only member no delivery attempted
        verify(exactly = 0) { userRegistry.getServerId(any()) }
        // but message is still persisted
        verify { messageRepository.save("alice", "conv-1", "hello") }
    }

    @Test
    fun `route delivers to all recipients except sender`() {
        every { conversationRegistry.getMembers("conv-1") } returns listOf("alice", "bob", "charlie")
        every { userRegistry.getServerId("bob") } returns "server-2"
        every { userRegistry.getServerId("charlie") } returns null
        every { serverRegistry.getQueueUrl("server-2") } returns "https://sqs/queue-2"

        router.route("alice", "conv-1", "group message")

        verify(exactly = 1) { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) }
        verify { pendingMessageRepository.save(ChatMessage("alice", "charlie", "conv-1", "group message")) }
    }

    @Test
    fun `route handles multiple recipients all offline`() {
        every { conversationRegistry.getMembers("conv-1") } returns listOf("alice", "bob", "charlie")
        every { userRegistry.getServerId("bob") } returns null
        every { userRegistry.getServerId("charlie") } returns null

        router.route("alice", "conv-1", "hello")

        verify { pendingMessageRepository.save(ChatMessage("alice", "bob", "conv-1", "hello")) }
        verify { pendingMessageRepository.save(ChatMessage("alice", "charlie", "conv-1", "hello")) }
        verify(exactly = 0) { sqsClient.sendMessage(any<Consumer<SendMessageRequest.Builder>>()) }
    }
}
