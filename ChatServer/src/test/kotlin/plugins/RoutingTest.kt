package org.chatserver.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
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

private const val ROUTING_TEST_SECRET = "routing-test-secret"

private fun routingToken(subject: String): String =
    JWT.create().withSubject(subject).sign(Algorithm.HMAC256(ROUTING_TEST_SECRET))

class RoutingTest {
    @Test
    fun `GET conversations returns 200 with stored messages`() {
        val repo = mockk<MessageRepository>()
        val presenceClient = mockk<PresenceClient>()
        every { repo.getMessages("conv-1") } returns
            listOf(
                StoredMessage("alice", "conv-1", "hello", "2024-01-01T00:00:00Z"),
                StoredMessage("bob", "conv-1", "hi", "2024-01-01T00:00:01Z"),
            )

        testApplication {
            application {
                this.install(Koin) { modules(module { single { repo }; single { presenceClient } }) }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            val response = client.get("/conversations/conv-1/messages") {
                header("Authorization", "Bearer ${routingToken("alice")}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assert(body.contains("alice"))
            assert(body.contains("hello"))
        }
    }

    @Test
    fun `GET conversations returns empty JSON array when no messages exist`() {
        val repo = mockk<MessageRepository>()
        val presenceClient = mockk<PresenceClient>()
        every { repo.getMessages("empty-conv") } returns emptyList()

        testApplication {
            application {
                this.install(Koin) { modules(module { single { repo }; single { presenceClient } }) }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            val response = client.get("/conversations/empty-conv/messages") {
                header("Authorization", "Bearer ${routingToken("alice")}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[]", response.bodyAsText())
        }
    }

    @Test
    fun `GET conversations response deserializes to StoredMessage list`() {
        val repo = mockk<MessageRepository>()
        val presenceClient = mockk<PresenceClient>()
        val expected = listOf(StoredMessage("alice", "conv-1", "test content", "2024-01-01T00:00:00Z"))
        every { repo.getMessages("conv-1") } returns expected

        testApplication {
            application {
                this.install(Koin) { modules(module { single { repo }; single { presenceClient } }) }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            val response = client.get("/conversations/conv-1/messages") {
                header("Authorization", "Bearer ${routingToken("alice")}")
            }
            val parsed = Json.decodeFromString<List<StoredMessage>>(response.bodyAsText())
            assertEquals(expected, parsed)
        }
    }

    @Test
    fun `GET conversations returns 401 without JWT`() {
        val repo = mockk<MessageRepository>()
        val presenceClient = mockk<PresenceClient>()

        testApplication {
            application {
                this.install(Koin) { modules(module { single { repo }; single { presenceClient } }) }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            val response = client.get("/conversations/conv-1/messages")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `GET health returns 200 with ok status`() {
        val repo = mockk<MessageRepository>()
        val presenceClient = mockk<PresenceClient>()

        testApplication {
            application {
                install(Koin) { modules(module { single { repo }; single { presenceClient } }) }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            val response = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("ok", Json.decodeFromString<HealthResponse>(response.bodyAsText()).status)
        }
    }

    @Test
    fun `POST presence batch returns presence map with valid internal key`() {
        val repo = mockk<MessageRepository>()
        val presenceClient = mockk<PresenceClient>()
        coEvery { presenceClient.batchPresence(listOf("user-1", "user-2")) } returns
            mapOf("user-1" to true, "user-2" to false)

        testApplication {
            application {
                install(Koin) { modules(module { single { repo }; single { presenceClient } }) }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            val response = client.post("/presence/batch") {
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
        val repo = mockk<MessageRepository>()
        val presenceClient = mockk<PresenceClient>()

        testApplication {
            application {
                install(Koin) { modules(module { single { repo }; single { presenceClient } }) }
                configureSerialization()
                configureAuth(ROUTING_TEST_SECRET)
                configureRouting(ROUTING_TEST_SECRET)
            }
            val response = client.post("/presence/batch") {
                contentType(ContentType.Application.Json)
                setBody("""{"user_ids":["user-1"]}""")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }
}
