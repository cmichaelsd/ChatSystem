package org.chatserver.plugins

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.chatserver.data.repository.MessageRepository
import org.chatserver.models.HealthResponse
import org.chatserver.models.StoredMessage
import org.chatserver.services.presence.PresenceClient
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutingTest {
    companion object {
        private const val ROUTING_TEST_SECRET = "routing-test-secret"
    }

    private val repo = mockk<MessageRepository>()
    private val presenceClient = mockk<PresenceClient>()

    private fun withRoutingApp(block: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            application {
                install(Koin) {
                    modules(
                        module {
                            single { repo }
                            single { presenceClient }
                        },
                    )
                }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            block()
        }
    }

    @Test
    fun `GET conversations returns 200 with stored messages`() {
        every { repo.getMessages("conv-1") } returns
            listOf(
                StoredMessage("alice", "conv-1", "hello", "2024-01-01T00:00:00Z"),
                StoredMessage("bob", "conv-1", "hi", "2024-01-01T00:00:01Z"),
            )

        withRoutingApp {
            val response =
                client.get("/conversations/conv-1/messages") {
                    header("x-internal-key", ROUTING_TEST_SECRET)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assert(body.contains("alice"))
            assert(body.contains("hello"))
        }
    }

    @Test
    fun `GET conversations returns empty JSON array when no messages exist`() {
        every { repo.getMessages("empty-conv") } returns emptyList()

        withRoutingApp {
            val response =
                client.get("/conversations/empty-conv/messages") {
                    header("x-internal-key", ROUTING_TEST_SECRET)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[]", response.bodyAsText())
        }
    }

    @Test
    fun `GET conversations response deserializes to StoredMessage list`() {
        val expected = listOf(StoredMessage("alice", "conv-1", "test content", "2024-01-01T00:00:00Z"))
        every { repo.getMessages("conv-1") } returns expected

        withRoutingApp {
            val response =
                client.get("/conversations/conv-1/messages") {
                    header("x-internal-key", ROUTING_TEST_SECRET)
                }
            val parsed = Json.decodeFromString<List<StoredMessage>>(response.bodyAsText())
            assertEquals(expected, parsed)
        }
    }

    @Test
    fun `GET conversations returns 403 without internal key`() {
        withRoutingApp {
            val response = client.get("/conversations/conv-1/messages")
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    @Test
    fun `GET health returns 200 with ok status`() {
        withRoutingApp {
            val response = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ok", Json.decodeFromString<HealthResponse>(response.bodyAsText()).status)
        }
    }

    @Test
    fun `POST presence batch returns presence map with valid internal key`() {
        coEvery { presenceClient.batchPresence(listOf("user-1", "user-2")) } returns
            mapOf("user-1" to true, "user-2" to false)

        withRoutingApp {
            val response =
                client.post("/presence/batch") {
                    contentType(ContentType.Application.Json)
                    header("x-internal-key", ROUTING_TEST_SECRET)
                    setBody("""{"user_ids":["user-1","user-2"]}""")
                }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assert(body.contains("user-1"))
            assert(body.contains("user-2"))
        }
    }

    @Test
    fun `POST presence batch returns 403 without internal key`() {
        withRoutingApp {
            val response =
                client.post("/presence/batch") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"user_ids":["user-1"]}""")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }
}
