package org.chatserver

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import org.chatserver.plugins.configureDI
import org.chatserver.plugins.configureDevRoutes
import org.chatserver.plugins.configureRouting
import org.chatserver.plugins.configureSerialization
import org.chatserver.plugins.configureSockets
import org.chatserver.plugins.configureSqs

fun main() {
    embeddedServer(CIO, port = 8080) {
        configureDI()
        configureSerialization()
        configureSqs()
        configureSockets()
        configureRouting()
        configureDevRoutes()
    }.start(wait = true)
}
