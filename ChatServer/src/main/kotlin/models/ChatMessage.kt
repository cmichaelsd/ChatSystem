package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val type: String = "chat",
    val fromUserId: String,
    val toUserId: String,
    val conversationId: String,
    val content: String,
)
