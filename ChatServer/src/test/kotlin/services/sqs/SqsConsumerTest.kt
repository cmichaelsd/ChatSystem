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
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.ChatMessage
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
    private val consumer = SqsConsumer(sqsClient, sessionStore, pendingMessageRepository)

    private val chatMessage = ChatMessage("alice", "bob", "conv-1", "hello")
    private val messageJson = Json.encodeToString(chatMessage)

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
                    .messages(buildSqsMessage(messageJson))
                    .build(),
            )
            every { sessionStore.get("bob") } returns session

            val job = launch { consumer.start("https://sqs/queue") }
            job.join()

            coVerify { session.send(match { it is Frame.Text && (it as Frame.Text).readText() == messageJson }) }
            verify(exactly = 0) { pendingMessageRepository.save(any()) }
        }

    @Test
    fun `saves message to pending when recipient session is gone`() =
        runTest {
            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(buildSqsMessage(messageJson))
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
                    .messages(buildSqsMessage(messageJson, "handle-abc"))
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
                    .messages(buildSqsMessage(messageJson))
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
            val msg1 = ChatMessage("alice", "bob", "conv-1", "hello")
            val msg2 = ChatMessage("alice", "bob", "conv-1", "world")
            val session = mockk<DefaultWebSocketServerSession>()
            coEvery { session.send(any<Frame>()) } returns Unit

            stubReceiveOnce(
                ReceiveMessageResponse.builder()
                    .messages(
                        buildSqsMessage(Json.encodeToString(msg1), "r1"),
                        buildSqsMessage(Json.encodeToString(msg2), "r2"),
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
}
