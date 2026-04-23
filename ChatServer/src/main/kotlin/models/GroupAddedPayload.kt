package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class GroupAddedPayload(
    val conversationId: String,
    val userId: String,
)
