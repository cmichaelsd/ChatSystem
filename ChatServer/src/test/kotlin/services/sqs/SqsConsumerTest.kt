package org.chatserver.services.sqs

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.ChatMessage
import org.chatserver.models.PresenceEvent
import org.chatserver.models.SqsEnvelope
import org.chatserver.session.SessionStore
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import java.util.function.Consumer
import kotlin.test.Test

class SqsConsumerTest {
    private val sqsClient = mockk<SqsClient>(relaxed = true)
    private val sessionStore = mockk<SessionStore>()
    private val pendingMessageRepository = mockk<PendingMessageRepository>(relaxed = true)
    private val conversationRegistry = mockk<ConversationRegistry>()
    private val consumer = SqsConsumer(sqsClient, sessionStore, pendingMessageRepository, conversationRegistry)

    private val chatMessage = ChatMessage(fromUserId = "alice", toUserId = "bob", conversationId = "conv-1", content = "hello")
    private val chatMessageJson = Json.encodeToString(chatMessage)
    private val chatEnvelopeJson = Json.encodeToString(SqsEnvelope(SqsEnvelope.CHAT, chatMessageJson))

    /** Stubs receiveMessage to return [response] once, then throw CancellationException to stop the loop. */
    private fun stubReceiveOnce(response: ReceiveMessageResponse) {
        var callCount = 0
        every { sqsClient.receiveMessage(any<Consumer<ReceiveMessageRequest.Builder>>()) } answers {
            if (callCount++ == 0) response else throw CancellationException("test done")
        }
    }

    private fun buildSqsMessage(
        body: String,
        receiptHandle: String = "receipt-1",
    ): Message = Message.builder().body(body).receiptHandle(receiptHandle).build()

    @Test
    fun `delivers message via WebSocket when recipient session is active`() =
        runTest {
            val session = mockk<DefaultWebSocketServerSession>()
            coEvery { session.send(any<Frame>()) } returns Unit

            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(buildSqsMessage(chatEnvelopeJson))
                    .build(),
            )
            every { sessionStore.get("bob") } returns session

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            coVerify { session.send(match { it is Frame.Text && (it as Frame.Text).readText() == chatMessageJson }) }
            verify(exactly = 0) { pendingMessageRepository.save(any()) }
        }

    @Test
    fun `saves message to pending when recipient session is gone`() =
        runTest {
            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(buildSqsMessage(chatEnvelopeJson))
                    .build(),
            )
            every { sessionStore.get("bob") } returns null

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            verify { pendingMessageRepository.save(chatMessage) }
        }

    @Test
    fun `always deletes SQS message after delivering to session`() =
        runTest {
            val session = mockk<DefaultWebSocketServerSession>()
            coEvery { session.send(any<Frame>()) } returns Unit

            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(buildSqsMessage(chatEnvelopeJson, "handle-abc"))
                    .build(),
            )
            every { sessionStore.get("bob") } returns session

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            verify(exactly = 1) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
        }

    @Test
    fun `always deletes SQS message after saving to pending`() =
        runTest {
            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(buildSqsMessage(chatEnvelopeJson))
                    .build(),
            )
            every { sessionStore.get("bob") } returns null

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            verify(exactly = 1) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
        }

    @Test
    fun `processes multiple messages in a single batch`() =
        runTest {
            val msg1 = ChatMessage(fromUserId = "alice", toUserId = "bob", conversationId = "conv-1", content = "hello")
            val msg2 = ChatMessage(fromUserId = "alice", toUserId = "bob", conversationId = "conv-1", content = "world")
            val env1 = Json.encodeToString(SqsEnvelope(SqsEnvelope.CHAT, Json.encodeToString(msg1)))
            val env2 = Json.encodeToString(SqsEnvelope(SqsEnvelope.CHAT, Json.encodeToString(msg2)))
            val session = mockk<DefaultWebSocketServerSession>()
            coEvery { session.send(any<Frame>()) } returns Unit

            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(
                        buildSqsMessage(env1, "r1"),
                        buildSqsMessage(env2, "r2"),
                    )
                    .build(),
            )
            every { sessionStore.get("bob") } returns session

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            coVerify(exactly = 2) { session.send(any<Frame>()) }
            verify(exactly = 2) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
        }

    @Test
    fun `empty batch does not interact with sessions or pending repo`() =
        runTest {
            stubReceiveOnce(
                ReceiveMessageResponse.builder().messages(emptyList()).build(),
            )

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            verify(exactly = 0) { sessionStore.get(any()) }
            verify(exactly = 0) { pendingMessageRepository.save(any()) }
            verify(exactly = 0) { sqsClient.deleteMessage(any<Consumer<DeleteMessageRequest.Builder>>()) }
        }

    @Test
    fun `presence event is delivered only to groupmates of the sender`() =
        runTest {
            val groupmateSession = mockk<DefaultWebSocketServerSession>()
            val nonGroupmateSession = mockk<DefaultWebSocketServerSession>()
            coEvery { groupmateSession.send(any<Frame>()) } returns Unit
            coEvery { nonGroupmateSession.send(any<Frame>()) } returns Unit

            val presenceEvent = PresenceEvent(userId = "charlie", online = true)
            val presenceEnvelopeJson =
                Json.encodeToString(SqsEnvelope(SqsEnvelope.PRESENCE, Json.encodeToString(presenceEvent)))

            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(buildSqsMessage(presenceEnvelopeJson))
                    .build(),
            )
            every { conversationRegistry.getGroupmates("charlie") } returns setOf("alice")
            every { sessionStore.getAll() } returns setOf("alice", "dave")
            every { sessionStore.get("alice") } returns groupmateSession

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            coVerify(exactly = 1) { groupmateSession.send(any<Frame>()) }
            coVerify(exactly = 0) { nonGroupmateSession.send(any<Frame>()) }
            verify(exactly = 0) { pendingMessageRepository.save(any()) }
        }
}
