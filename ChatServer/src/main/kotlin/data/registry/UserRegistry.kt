package org.chatserver.data.registry

import org.chatserver.data.AbstractDynamoRegistry
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest

class UserRegistry(
    dynamoClient: DynamoDbClient,
    private val serverId: String,
) : AbstractDynamoRegistry(dynamoClient) {
    override val tableName = "UserConnections"
    override val partitionKey = "userId"

    private val logger = LoggerFactory.getLogger(UserRegistry::class.java)

    fun init() {
        ensureTableExists()
    }

    fun register(userId: String) {
        dynamoClient.putItem(
            PutItemRequest.builder()
                .tableName(tableName)
                .item(
                    mapOf(
                        "userId" to AttributeValue.fromS(userId),
                        "serverId" to AttributeValue.fromS(serverId),
                    ),
                )
                .build(),
        )
        logger.info("User $userId registered on server $serverId")
    }

    fun getServerId(userId: String): String? {
        val response =
            dynamoClient.getItem {
                it.tableName(tableName)
                it.key(mapOf("userId" to AttributeValue.fromS(userId)))
            }
        return response.item()["serverId"]?.s()
    }

    fun getAllConnectedUserIds(): List<String> {
        val response =
            dynamoClient.scan(
                ScanRequest.builder()
                    .tableName(tableName)
                    .projectionExpression("userId")
                    .build(),
            )
        return response.items().mapNotNull { it["userId"]?.s() }
    }

    fun getConnectedUsersFrom(userIds: Set<String>): Set<String> {
        if (userIds.isEmpty()) return emptySet()
        val connected = mutableSetOf<String>()
        userIds.chunked(100).forEach { chunk ->
            val keys = chunk.map { mapOf("userId" to AttributeValue.fromS(it)) }
            val response =
                dynamoClient.batchGetItem {
                    it.requestItems(mapOf(tableName to KeysAndAttributes.builder().keys(keys).build()))
                }
            response.responses()[tableName]?.forEach { item ->
                item["userId"]?.s()?.let { id -> connected.add(id) }
            }
        }
        return connected
    }

    fun deregister(userId: String): Boolean {
        return try {
            dynamoClient.deleteItem(
                DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(mapOf("userId" to AttributeValue.fromS(userId)))
                    .conditionExpression("serverId = :sid")
                    .expressionAttributeValues(mapOf(":sid" to AttributeValue.fromS(serverId)))
                    .build(),
            )
            logger.info("User $userId deregistered from server $serverId")
            true
        } catch (e: software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException) {
            logger.info("User $userId already claimed by another server, skipping deregister")
            false
        }
    }
}
