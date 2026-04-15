package org.chatserver.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatMessage(
    @EncodeDefault val type: String = "chat",
    val fromUserId: String,
    val toUserId: String,
    val conversationId: String,
    val content: String,
)
