package org.chatserver.data.registry

import org.chatserver.data.AbstractDynamoCompositeRegistry
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest

class ConversationRegistry(dynamoClient: DynamoDbClient) : AbstractDynamoCompositeRegistry(dynamoClient) {
    override val tableName = "ConversationMembers"
    override val partitionKey = "conversationId"
    override val sortKey = "userId"

    companion object {
        private const val USER_ID_INDEX = "userId-index"
    }

    // userId is already an attribute (table sort key), so no extra attribute definition needed.
    override fun gsiDefinitions(): List<GlobalSecondaryIndex> =
        listOf(
            GlobalSecondaryIndex.builder()
                .indexName(USER_ID_INDEX)
                .keySchema(KeySchemaElement.builder().attributeName(sortKey).keyType(KeyType.HASH).build())
                .projection(Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                .build(),
        )

    override fun gsiAttributeDefinitions(): List<AttributeDefinition> = emptyList()

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

    fun getConversationsForUser(userId: String): List<String> {
        val response =
            dynamoClient.query(
                QueryRequest.builder()
                    .tableName(tableName)
                    .indexName(USER_ID_INDEX)
                    .keyConditionExpression("$sortKey = :uid")
                    .expressionAttributeValues(mapOf(":uid" to AttributeValue.fromS(userId)))
                    .build(),
            )
        return response.items().mapNotNull { it[partitionKey]?.s() }
    }

    fun getGroupmates(userId: String): Set<String> {
        val conversationIds = getConversationsForUser(userId)
        return conversationIds
            .flatMap { getMembers(it) }
            .toSet()
            .minus(userId)
    }
}
