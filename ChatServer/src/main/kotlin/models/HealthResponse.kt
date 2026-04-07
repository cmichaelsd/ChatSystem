package org.chatserver.models

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String)
