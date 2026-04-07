package org.chatserver.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import org.chatserver.data.registry.UserRegistry
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.models.ChatMessage
import org.chatserver.models.InboundMessage
import org.chatserver.routing.MessageRouter
import org.chatserver.session.SessionStore
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

private const val TEST_SECRET = "test-secret"

private fun token(subject: String): String = JWT.create().withSubject(subject).sign(Algorithm.HMAC256(TEST_SECRET))

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
                install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureAuth(TEST_SECRET)
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?token=${token("alice")}") { close() }
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
                install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureAuth(TEST_SECRET)
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?token=${token("alice")}") {
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
                install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureAuth(TEST_SECRET)
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?token=${token("alice")}") {
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
                install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureAuth(TEST_SECRET)
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            wsClient.webSocket("/chat?token=${token("alice")}") {
                send(Frame.Text("not valid json {{{}}}"))
                close()
            }
        }

        verify(exactly = 0) { messageRouter.route(any(), any(), any()) }
    }

    @Test
    fun `rejects connection when token is missing`() {
        val userRegistry = mockk<UserRegistry>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val messageRouter = mockk<MessageRouter>(relaxed = true)
        val pendingRepo = mockk<PendingMessageRepository>(relaxed = true)

        testApplication {
            application {
                install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureAuth(TEST_SECRET)
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            runCatching { wsClient.webSocket("/chat") { } }
        }

        verify(exactly = 0) { userRegistry.register(any()) }
    }

    @Test
    fun `rejects connection when token is signed with wrong secret`() {
        val userRegistry = mockk<UserRegistry>(relaxed = true)
        val sessionStore = mockk<SessionStore>(relaxed = true)
        val messageRouter = mockk<MessageRouter>(relaxed = true)
        val pendingRepo = mockk<PendingMessageRepository>(relaxed = true)
        val badToken = JWT.create().withSubject("alice").sign(Algorithm.HMAC256("wrong-secret"))

        testApplication {
            application {
                install(Koin) {
                    modules(
                        module {
                            single { userRegistry }
                            single { sessionStore }
                            single { messageRouter }
                            single { pendingRepo }
                        },
                    )
                }
                configureAuth(TEST_SECRET)
                configureSockets()
            }
            val wsClient = createClient { install(ClientWebSockets) }
            runCatching { wsClient.webSocket("/chat?token=$badToken") { } }
        }

        verify(exactly = 0) { userRegistry.register(any()) }
    }
}
