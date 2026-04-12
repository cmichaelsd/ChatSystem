package org.chatserver

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import org.chatserver.plugins.configureAuth
import org.chatserver.plugins.configureConversationRoutes
import org.chatserver.plugins.configureCors
import org.chatserver.plugins.configureDI
import org.chatserver.plugins.configurePresence
import org.chatserver.plugins.configureRouting
import org.chatserver.plugins.configureSerialization
import org.chatserver.plugins.configureShutdown
import org.chatserver.plugins.configureSockets
import org.chatserver.plugins.configureSqs

fun main() {
    embeddedServer(CIO, port = 8080) {
        configureDI()
        configureCors()
        configureSerialization()
        configureSqs()
        configureAuth()
        configureSockets()
        configurePresence()
        configureRouting()
        configureConversationRoutes()
        configureShutdown()
    }.start(wait = true)
}
