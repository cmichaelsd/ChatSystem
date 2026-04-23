package org.chatserver.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GroupAddedEvent(
    @EncodeDefault val type: String = "group_added",
    val conversationId: String,
)
