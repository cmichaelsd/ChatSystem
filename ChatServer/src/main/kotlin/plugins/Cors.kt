package org.chatserver.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors() {
    val rawOrigins = System.getenv("CORS_ORIGINS") ?: "http://localhost:5173"
    val allowedOrigins = rawOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    install(CORS) {
        allowedOrigins.forEach {
            allowHost(
                it.removePrefix("http://").removePrefix("https://"),
                schemes = listOf(it.substringBefore("://")),
            )
        }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
}
