package org.chatserver.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import org.chatserver.data.registry.ServerRegistry
import org.koin.ktor.ext.inject

fun Application.configureShutdown() {
    val serverRegistry by inject<ServerRegistry>()

    environment.monitor.subscribe(ApplicationStopping) {
        serverRegistry.deregister()
    }
}
