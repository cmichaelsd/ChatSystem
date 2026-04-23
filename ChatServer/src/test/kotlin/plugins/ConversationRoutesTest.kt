package org.chatserver.plugins

import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.data.registry.ServerRegistry
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import software.amazon.awssdk.services.sqs.SqsClient
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationRoutesTest {
    companion object {
        private const val TEST_INTERNAL_KEY = "test-internal-key"
    }

    private val registry = mockk<ConversationRegistry>(relaxed = true)
    private val serverRegistry = mockk<ServerRegistry>(relaxed = true)
    private val sqsClient = mockk<SqsClient>(relaxed = true)

    private fun withConversationApp(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            application {
                install(Koin) {
                    modules(
                        module {
                            single { registry }
                            single { serverRegistry }
                            single { sqsClient }
                        },
                    )
                }
                configureSerialization()
                configureConversationRoutes(TEST_INTERNAL_KEY)
            }
            block()
        }
    }

    @Test
    fun `POST conversations returns 201 Created`() {
        withConversationApp {
            val response =
                client.post("/conversations") {
                    contentType(ContentType.Application.Json)
                    header("x-internal-key", TEST_INTERNAL_KEY)
                    setBody("""{"conversationId":"conv-1","memberIds":["alice","bob"]}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `POST conversations calls addMember for each member`() {
        withConversationApp {
            client.post("/conversations") {
                contentType(ContentType.Application.Json)
                header("x-internal-key", TEST_INTERNAL_KEY)
                setBody("""{"conversationId":"conv-1","memberIds":["alice","bob","charlie"]}""")
            }
        }

        verify { registry.addMember("conv-1", "alice") }
        verify { registry.addMember("conv-1", "bob") }
        verify { registry.addMember("conv-1", "charlie") }
    }

    @Test
    fun `POST conversations with empty member list returns 201`() {
        withConversationApp {
            val response =
                client.post("/conversations") {
                    contentType(ContentType.Application.Json)
                    header("x-internal-key", TEST_INTERNAL_KEY)
                    setBody("""{"conversationId":"empty-conv","memberIds":[]}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }

        verify(exactly = 0) { registry.addMember(any(), any()) }
    }

    @Test
    fun `POST conversations members userId returns 201 and calls addMember`() {
        every { serverRegistry.getAllQueueUrls() } returns emptyList()

        withConversationApp {
            val response =
                client.post("/conversations/conv-1/members/alice") {
                    header("x-internal-key", TEST_INTERNAL_KEY)
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }

        verify { registry.addMember("conv-1", "alice") }
    }

    @Test
    fun `POST conversations members userId broadcasts GROUP_ADDED via SQS`() {
        every { serverRegistry.getAllQueueUrls() } returns listOf("https://sqs/queue-1")

        withConversationApp {
            client.post("/conversations/conv-1/members/alice") {
                header("x-internal-key", TEST_INTERNAL_KEY)
            }
        }

        verify(exactly = 1) {
            sqsClient.sendMessage(any<java.util.function.Consumer<software.amazon.awssdk.services.sqs.model.SendMessageRequest.Builder>>())
        }
    }

    @Test
    fun `DELETE conversations members userId returns 204 and calls removeMember`() {
        withConversationApp {
            val response =
                client.delete("/conversations/conv-1/members/alice") {
                    header("x-internal-key", TEST_INTERNAL_KEY)
                }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }

        verify { registry.removeMember("conv-1", "alice") }
    }

    @Test
    fun `POST conversations without internal key returns 403`() {
        withConversationApp {
            val response =
                client.post("/conversations") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"conversationId":"conv-1","memberIds":[]}""")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }
}
