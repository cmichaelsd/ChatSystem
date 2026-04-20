package org.chatserver.data.repository

import org.chatserver.data.AbstractDynamoCompositeRegistry
import org.chatserver.models.ChatMessage
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.WriteRequest
import java.time.Instant
import java.util.UUID

class PendingMessageRepository(dynamoClient: DynamoDbClient) : AbstractDynamoCompositeRegistry(dynamoClient) {
    override val tableName = "PendingMessages"
    override val partitionKey = "userId"
    override val sortKey = "messageId"

    private val logger = LoggerFactory.getLogger(PendingMessageRepository::class.java)

    fun init() {
        ensureTableExists()
    }

    fun save(message: ChatMessage) {
        dynamoClient.putItem(
            PutItemRequest.builder()
                .tableName(tableName)
                .item(
                    mapOf(
                        partitionKey to AttributeValue.fromS(message.toUserId),
                        sortKey to AttributeValue.fromS(UUID.randomUUID().toString()),
                        "fromUserId" to AttributeValue.fromS(message.fromUserId),
                        "conversationId" to AttributeValue.fromS(message.conversationId),
                        "content" to AttributeValue.fromS(message.content),
                        "sentAt" to AttributeValue.fromS(Instant.now().toString()),
                    ),
                )
                .build(),
        )
        logger.info("Saved pending message for user ${message.toUserId}")
    }

    fun fetchAndClear(userId: String): List<ChatMessage> {
        val response =
            dynamoClient.query(
                QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("$partitionKey = :uid")
                    .expressionAttributeValues(mapOf(":uid" to AttributeValue.fromS(userId)))
                    .build(),
            )

        val messages =
            response.items().map { item ->
                ChatMessage(
                    fromUserId = item["fromUserId"]!!.s(),
                    toUserId = userId,
                    conversationId = item["conversationId"]!!.s(),
                    content = item["content"]!!.s(),
                )
            }

        response.items().chunked(25).forEach { chunk ->
            dynamoClient.batchWriteItem(
                BatchWriteItemRequest.builder()
                    .requestItems(
                        mapOf(
                            tableName to chunk.map { item ->
                                WriteRequest.builder()
                                    .deleteRequest(
                                        DeleteRequest.builder()
                                            .key(mapOf(partitionKey to item[partitionKey]!!, sortKey to item[sortKey]!!))
                                            .build(),
                                    )
                                    .build()
                            },
                        ),
                    )
                    .build(),
            )
        }

        return messages
    }
}
