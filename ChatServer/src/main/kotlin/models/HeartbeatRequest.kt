package org.chatserver.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatRequest(
    @SerialName("user_ids") val userIds: List<String>,
)
