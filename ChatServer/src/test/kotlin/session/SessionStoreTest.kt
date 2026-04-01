package org.chatserver.session

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionStoreTest {
    private val store = SessionStore()

    @Test
    fun `get returns null for unknown user`() {
        assertNull(store.get("alice"))
    }

    @Test
    fun `add and get round-trips session`() {
        val session = mockk<DefaultWebSocketServerSession>()
        store.add("alice", session)
        assertEquals(session, store.get("alice"))
    }

    @Test
    fun `remove makes session unavailable`() {
        val session = mockk<DefaultWebSocketServerSession>()
        store.add("alice", session)
        store.remove("alice")
        assertNull(store.get("alice"))
    }

    @Test
    fun `remove is idempotent for unknown user`() {
        store.remove("nobody") // should not throw
        assertNull(store.get("nobody"))
    }

    @Test
    fun `add overwrites existing session for same user`() {
        val sessionA = mockk<DefaultWebSocketServerSession>()
        val sessionB = mockk<DefaultWebSocketServerSession>()
        store.add("alice", sessionA)
        store.add("alice", sessionB)
        assertEquals(sessionB, store.get("alice"))
    }

    @Test
    fun `multiple users are stored independently`() {
        val sessionAlice = mockk<DefaultWebSocketServerSession>()
        val sessionBob = mockk<DefaultWebSocketServerSession>()
        store.add("alice", sessionAlice)
        store.add("bob", sessionBob)
        assertEquals(sessionAlice, store.get("alice"))
        assertEquals(sessionBob, store.get("bob"))
    }

    @Test
    fun `remove only affects the specified user`() {
        val sessionAlice = mockk<DefaultWebSocketServerSession>()
        val sessionBob = mockk<DefaultWebSocketServerSession>()
        store.add("alice", sessionAlice)
        store.add("bob", sessionBob)
        store.remove("alice")
        assertNull(store.get("alice"))
        assertEquals(sessionBob, store.get("bob"))
    }
}
