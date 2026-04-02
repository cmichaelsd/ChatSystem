package org.chatserver.plugins

import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import io.mockk.verify
import org.chatserver.registry.ConversationRegistry
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationRoutesTest {
    @Test
    fun `POST conversations returns 201 Created`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureConversationRoutes()
            }
            val response =
                client.post("/conversations") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"conversationId":"conv-1","memberIds":["alice","bob"]}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `POST conversations calls addMember for each member`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureConversationRoutes()
            }
            client.post("/conversations") {
                contentType(ContentType.Application.Json)
                setBody("""{"conversationId":"conv-1","memberIds":["alice","bob","charlie"]}""")
            }
        }

        verify { registry.addMember("conv-1", "alice") }
        verify { registry.addMember("conv-1", "bob") }
        verify { registry.addMember("conv-1", "charlie") }
    }

    @Test
    fun `POST conversations with empty member list returns 201`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureConversationRoutes()
            }
            val response =
                client.post("/conversations") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"conversationId":"empty-conv","memberIds":[]}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }

        verify(exactly = 0) { registry.addMember(any(), any()) }
    }

    @Test
    fun `POST conversations members userId returns 201 and calls addMember`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureConversationRoutes()
            }
            val response = client.post("/conversations/conv-1/members/alice")
            assertEquals(HttpStatusCode.Created, response.status)
        }

        verify { registry.addMember("conv-1", "alice") }
    }

    @Test
    fun `DELETE conversations members userId returns 204 and calls removeMember`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureConversationRoutes()
            }
            val response = client.delete("/conversations/conv-1/members/alice")
            assertEquals(HttpStatusCode.NoContent, response.status)
        }

        verify { registry.removeMember("conv-1", "alice") }
    }
}
