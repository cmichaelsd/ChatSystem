package org.chatserver.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chatserver.services.sqs.SqsConsumer
import org.chatserver.services.sqs.SqsQueueManager
import org.koin.ktor.ext.inject

fun Application.configureSqs() {
    val sqsQueueManager by inject<SqsQueueManager>()
    val sqsConsumer by inject<SqsConsumer>()

    val queueUrl = sqsQueueManager.init()

    launch(Dispatchers.IO) {
        sqsConsumer.start(queueUrl)
    }

    environment.monitor.subscribe(ApplicationStopping) {
        sqsQueueManager.delete(queueUrl)
    }
}
