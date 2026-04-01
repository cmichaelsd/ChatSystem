package org.chatserver.plugins

import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.model.ChatMessage
import org.chatserver.model.InboundMessage
import org.chatserver.registry.UserRegistry
import org.chatserver.repository.PendingMessageRepository
import org.chatserver.routing.MessageRouter
import org.chatserver.session.SessionStore
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

class SocketsTest {
    @Test
    fun `registers user and cleans up on disconnect`() {
        val userRegistry = mockk<UserRegistry>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val messageRouter = mockk<MessageRouter>(relaxed = true)
        val pendingRepo = mockk<PendingMessageRepository>(relaxed = true)
        every { pendingRepo.fetchAndClear("alice") } returns emptyList()

        testApplication {
            application {
                this.install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?user_id=alice") { close() }
        }

        verify { userRegistry.register("alice") }
        verify { sessionStore.add(eq("alice"), any()) }
        verify { userRegistry.deregister("alice") }
        verify { sessionStore.remove("alice") }
    }

    @Test
    fun `delivers pending messages on connect`() {
        val userRegistry = mockk<UserRegistry>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val messageRouter = mockk<MessageRouter>(relaxed = true)
        val pendingRepo = mockk<PendingMessageRepository>(relaxed = true)
        val pending = listOf(ChatMessage("bob", "alice", "conv-1", "pending mail"))
        every { pendingRepo.fetchAndClear("alice") } returns pending

        val received = mutableListOf<String>()

        testApplication {
            application {
                this.install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?user_id=alice") {
                withTimeout(2000) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) received.add(frame.readText())
                }
                close()
            }
        }

        assertEquals(1, received.size)
        assertEquals(pending[0], Json.decodeFromString<ChatMessage>(received[0]))
    }

    @Test
    fun `routes incoming text frames through MessageRouter`() {
        val userRegistry = mockk<UserRegistry>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val messageRouter = mockk<MessageRouter>(relaxed = true)
        val pendingRepo = mockk<PendingMessageRepository>(relaxed = true)
        every { pendingRepo.fetchAndClear("alice") } returns emptyList()

        testApplication {
            application {
                this.install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?user_id=alice") {
                send(Frame.Text(Json.encodeToString(InboundMessage("conv-1", "hello bob"))))
                close()
            }
        }

        verify { messageRouter.route("alice", "conv-1", "hello bob") }
    }

    @Test
    fun `ignores malformed JSON without crashing`() {
        val userRegistry = mockk<UserRegistry>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val messageRouter = mockk<MessageRouter>(relaxed = true)
        val pendingRepo = mockk<PendingMessageRepository>(relaxed = true)
        every { pendingRepo.fetchAndClear("alice") } returns emptyList()

        testApplication {
            application {
                this.install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?user_id=alice") {
                send(Frame.Text("not valid json {{{}}}"))
                close()
            }
        }

        verify(exactly = 0) { messageRouter.route(any(), any(), any()) }
    }

    @Test
    fun `closes connection when user_id query param is missing`() {
        val userRegistry = mockk<UserRegistry>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val messageRouter = mockk<MessageRouter>(relaxed = true)
        val pendingRepo = mockk<PendingMessageRepository>(relaxed = true)

        testApplication {
            application {
                this.install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat") {
                close()
            }
        }

        verify(exactly = 0) { userRegistry.register(any()) }
    }
}
