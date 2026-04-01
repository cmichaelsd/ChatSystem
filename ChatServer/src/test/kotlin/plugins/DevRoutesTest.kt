package org.chatserver.plugins

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

class DevRoutesTest {
    @Test
    fun `POST dev conversations returns 201 Created`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                this.install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureDevRoutes()
            }
            val response =
                client.post("/dev/conversations") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"conversationId":"conv-1","memberIds":["alice","bob"]}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `POST dev conversations calls addMember for each member`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                this.install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureDevRoutes()
            }
            client.post("/dev/conversations") {
                contentType(ContentType.Application.Json)
                setBody("""{"conversationId":"conv-1","memberIds":["alice","bob","charlie"]}""")
            }
        }

        verify { registry.addMember("conv-1", "alice") }
        verify { registry.addMember("conv-1", "bob") }
        verify { registry.addMember("conv-1", "charlie") }
    }

    @Test
    fun `POST dev conversations with empty member list calls addMember zero times`() {
        val registry = mockk<ConversationRegistry>(relaxed = true)

        testApplication {
            application {
                this.install(Koin) { modules(module { single { registry } }) }
                configureSerialization()
                configureDevRoutes()
            }
            val response =
                client.post("/dev/conversations") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"conversationId":"empty-conv","memberIds":[]}""")
                }
            assertEquals(HttpStatusCode.Created, response.status)
        }

        verify(exactly = 0) { registry.addMember(any(), any()) }
    }
}
