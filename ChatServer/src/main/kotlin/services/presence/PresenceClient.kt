package org.chatserver.services.presence

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import org.chatserver.models.HeartbeatRequest
import org.slf4j.LoggerFactory

class PresenceClient(
    private val presenceServerUrl: String,
    private val internalApiKey: String,
) {
    private val logger = LoggerFactory.getLogger(PresenceClient::class.java)
    private val client =
        HttpClient(CIO) {
            install(ContentNegotiation) { json() }
        }

    suspend fun sendHeartbeat(userIds: List<String>) {
        if (userIds.isEmpty()) return
        try {
            client.post("$presenceServerUrl/presence/heartbeat") {
                contentType(ContentType.Application.Json)
                header("x-internal-key", internalApiKey)
                setBody(HeartbeatRequest(userIds = userIds))
            }
        } catch (e: Exception) {
            logger.warn("Failed to send heartbeat to PresenceServer: ${e.message}")
        }
    }
}
