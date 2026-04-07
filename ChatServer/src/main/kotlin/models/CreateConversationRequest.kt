package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateConversationRequest(
    val conversationId: String,
    val memberIds: List<String>,
)
