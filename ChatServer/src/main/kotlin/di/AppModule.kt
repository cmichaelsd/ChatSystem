package org.chatserver.di

import org.chatserver.data.registry.ConversationRegistry
import org.chatserver.data.registry.ServerRegistry
import org.chatserver.data.registry.UserRegistry
import org.chatserver.data.repository.MessageRepository
import org.chatserver.data.repository.PendingMessageRepository
import org.chatserver.routing.MessageRouter
import org.chatserver.services.presence.PresenceClient
import org.chatserver.services.sqs.SqsConsumer
import org.chatserver.services.sqs.SqsQueueManager
import org.chatserver.session.SessionStore
import org.koin.core.qualifier.named
import org.koin.dsl.module
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI
import java.util.UUID

val appModule =
    module {

        single<String>(named("serverId")) { UUID.randomUUID().toString() }

        single {
            val endpoint = System.getenv("DYNAMO_ENDPOINT")
            DynamoDbClient.builder()
                .region(Region.US_WEST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .apply { if (endpoint != null) endpointOverride(URI.create(endpoint)) }
                .build()
        }

        single {
            val endpoint = System.getenv("SQS_ENDPOINT")
            SqsClient.builder()
                .region(Region.US_WEST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .apply { if (endpoint != null) endpointOverride(URI.create(endpoint)) }
                .build()
        }

        single { ServerRegistry(get(), get(named("serverId"))) }
        single { UserRegistry(get(), get(named("serverId"))) }
        single { SqsQueueManager(get(), get(), get(named("serverId"))) }
        single { PendingMessageRepository(get()) }
        single { ConversationRegistry(get()) }
        single { MessageRepository(get()) }
        single { SqsConsumer(get(), get(), get(), get()) }
        single { MessageRouter(get(), get(), get(), get(), get(), get()) }
        single { SessionStore() }
        single {
            PresenceClient(
                presenceServerUrl = System.getenv("PRESENCE_SERVER_URL") ?: "http://localhost:8002",
                internalApiKey = System.getenv("INTERNAL_API_KEY") ?: "",
            )
        }
    }
