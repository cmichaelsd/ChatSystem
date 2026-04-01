package org.chatserver.session

import io.ktor.server.websocket.DefaultWebSocketServerSession
import java.util.concurrent.ConcurrentHashMap

class SessionStore {
    private val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    fun add(
        userId: String,
        session: DefaultWebSocketServerSession,
    ) {
        sessions[userId] = session
    }

    fun remove(userId: String) {
        sessions.remove(userId)
    }

    fun get(userId: String): DefaultWebSocketServerSession? = sessions[userId]
}
