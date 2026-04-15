package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class PresenceEvent(
    val type: String = "presence",
    val userId: String,
    val online: Boolean,
)
