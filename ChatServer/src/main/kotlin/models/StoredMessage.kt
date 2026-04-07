package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class StoredMessage(
    val fromUserId: String,
    val conversationId: String,
    val content: String,
    val sentAt: String,
)
