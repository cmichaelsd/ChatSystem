package org.chatserver.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chatserver.services.presence.PresenceClient
import org.chatserver.session.SessionStore
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Presence")

fun Application.configurePresence() {
    val sessionStore by inject<SessionStore>()
    val presenceClient by inject<PresenceClient>()
    val intervalMs = System.getenv("PRESENCE_INTERVAL_MS")?.toLong() ?: 10_000L

    launch {
        while (true) {
            delay(intervalMs)
            val userIds = sessionStore.getAll().toList()
            if (userIds.isNotEmpty()) {
                logger.debug("Sending heartbeat for ${userIds.size} user(s)")
                presenceClient.sendHeartbeat(userIds)
            }
        }
    }
}
