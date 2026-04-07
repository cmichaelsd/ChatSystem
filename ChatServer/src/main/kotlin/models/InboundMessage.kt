package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class InboundMessage(
    val conversationId: String,
    val content: String,
)
