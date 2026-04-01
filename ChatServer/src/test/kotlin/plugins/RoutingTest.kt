package org.chatserver.plugins

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.chatserver.model.StoredMessage
import org.chatserver.repository.MessageRepository
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutingTest {
    @Test
    fun `GET conversations returns 200 with stored messages`() {
        val repo = mockk<MessageRepository>()
        every { repo.getMessages("conv-1") } returns
            listOf(
                StoredMessage("alice", "conv-1", "hello", "2024-01-01T00:00:00Z"),
                StoredMessage("bob", "conv-1", "hi", "2024-01-01T00:00:01Z"),
            )

        testApplication {
            application {
                this.install(Koin) { modules(module { single { repo } }) }
                configureSerialization()
                configureRouting()
            }
            val response = client.get("/conversations/conv-1/messages")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assert(body.contains("alice"))
            assert(body.contains("hello"))
        }
    }

    @Test
    fun `GET conversations returns empty JSON array when no messages exist`() {
        val repo = mockk<MessageRepository>()
        every { repo.getMessages("empty-conv") } returns emptyList()

        testApplication {
            application {
                this.install(Koin) { modules(module { single { repo } }) }
                configureSerialization()
                configureRouting()
            }
            val response = client.get("/conversations/empty-conv/messages")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("[]", response.bodyAsText())
        }
    }

    @Test
    fun `GET conversations response deserializes to StoredMessage list`() {
        val repo = mockk<MessageRepository>()
        val expected = listOf(StoredMessage("alice", "conv-1", "test content", "2024-01-01T00:00:00Z"))
        every { repo.getMessages("conv-1") } returns expected

        testApplication {
            application {
                this.install(Koin) { modules(module { single { repo } }) }
                configureSerialization()
                configureRouting()
            }
            val response = client.get("/conversations/conv-1/messages")
            val parsed = Json.decodeFromString<List<StoredMessage>>(response.bodyAsText())
            assertEquals(expected, parsed)
        }
    }
}
