package org.chatserver.data.registry

import org.chatserver.data.AbstractDynamoCompositeRegistry
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest

class ConversationRegistry(dynamoClient: DynamoDbClient) : AbstractDynamoCompositeRegistry(dynamoClient) {
    override val tableName = "ConversationMembers"
    override val partitionKey = "conversationId"
    override val sortKey = "userId"

    fun init() {
        ensureTableExists()
    }

    fun addMember(
        conversationId: String,
        userId: String,
    ) {
        dynamoClient.putItem(
            PutItemRequest.builder()
                .tableName(tableName)
                .item(
                    mapOf(
                        partitionKey to AttributeValue.fromS(conversationId),
                        sortKey to AttributeValue.fromS(userId),
                    ),
                )
                .build(),
        )
    }

    fun removeMember(
        conversationId: String,
        userId: String,
    ) {
        dynamoClient.deleteItem(
            DeleteItemRequest.builder()
                .tableName(tableName)
                .key(
                    mapOf(
                        partitionKey to AttributeValue.fromS(conversationId),
                        sortKey to AttributeValue.fromS(userId),
                    ),
                )
                .build(),
        )
    }

    fun getMembers(conversationId: String): List<String> {
        val response =
            dynamoClient.query(
                QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("$partitionKey = :cid")
                    .expressionAttributeValues(mapOf(":cid" to AttributeValue.fromS(conversationId)))
                    .build(),
            )
        return response.items().mapNotNull { it[sortKey]?.s() }
    }
}
