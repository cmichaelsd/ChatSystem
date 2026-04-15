package org.chatserver.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PresenceEvent(
    @EncodeDefault val type: String = "presence",
    val userId: String,
    val online: Boolean,
)
