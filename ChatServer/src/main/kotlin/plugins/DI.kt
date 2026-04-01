package org.chatserver.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import org.chatserver.di.appModule
import org.chatserver.registry.ConversationRegistry
import org.chatserver.registry.ServerRegistry
import org.chatserver.registry.UserRegistry
import org.chatserver.repository.MessageRepository
import org.chatserver.repository.PendingMessageRepository
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    val serverRegistry by inject<ServerRegistry>()
    serverRegistry.init()
    serverRegistry.register()

    val userRegistry by inject<UserRegistry>()
    userRegistry.init()

    val pendingMessageRepository by inject<PendingMessageRepository>()
    pendingMessageRepository.init()

    val conversationRegistry by inject<ConversationRegistry>()
    conversationRegistry.init()

    val messageRepository by inject<MessageRepository>()
    messageRepository.init()

    environment.monitor.subscribe(ApplicationStopped) {
        serverRegistry.deregister()
    }
}
