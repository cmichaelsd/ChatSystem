package org.chatserver.repository

import org.chatserver.model.StoredMessage
import org.chatserver.registry.DynamoCompositeRegistry
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import java.time.Instant

class MessageRepository(dynamoClient: DynamoDbClient) : DynamoCompositeRegistry(dynamoClient) {
    override val tableName = "Messages"
    override val partitionKey = "conversationId"
    override val sortKey = "sentAt"

    fun init() {
        ensureTableExists()
    }

    fun save(
        fromUserId: String,
        conversationId: String,
        content: String,
    ) {
        dynamoClient.putItem(
            PutItemRequest.builder()
                .tableName(tableName)
                .item(
                    mapOf(
                        partitionKey to AttributeValue.fromS(conversationId),
                        sortKey to AttributeValue.fromS(Instant.now().toString()),
                        "fromUserId" to AttributeValue.fromS(fromUserId),
                        "content" to AttributeValue.fromS(content),
                    ),
                )
                .build(),
        )
    }

    fun getMessages(conversationId: String): List<StoredMessage> {
        val response =
            dynamoClient.query(
                QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("$partitionKey = :cid")
                    .expressionAttributeValues(mapOf(":cid" to AttributeValue.fromS(conversationId)))
                    .build(),
            )

        return response.items().map { item ->
            StoredMessage(
                fromUserId = item["fromUserId"]!!.s(),
                conversationId = conversationId,
                content = item["content"]!!.s(),
                sentAt = item[sortKey]!!.s(),
            )
        }
    }
}
