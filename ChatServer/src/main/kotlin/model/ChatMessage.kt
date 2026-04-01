package org.chatserver.model

import kotlinx.serialization.Serializable

@Serializable
data class InboundMessage(
    val conversationId: String,
    val content: String,
)

@Serializable
data class ChatMessage(
    val fromUserId: String,
    val toUserId: String,
    val conversationId: String,
    val content: String,
)

@Serializable
data class StoredMessage(
    val fromUserId: String,
    val conversationId: String,
    val content: String,
    val sentAt: String,
)
