package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class SqsEnvelope(
    val type: String,
    val payload: String,
) {
    companion object {
        const val CHAT = "CHAT"
        const val PRESENCE = "PRESENCE"
    }
}
