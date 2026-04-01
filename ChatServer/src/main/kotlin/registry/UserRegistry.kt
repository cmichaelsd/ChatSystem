package org.chatserver.registry

import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest

class UserRegistry(
    dynamoClient: DynamoDbClient,
    private val serverId: String,
) : DynamoRegistry(dynamoClient) {
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

    fun deregister(userId: String) {
        dynamoClient.deleteItem(
            DeleteItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("userId" to AttributeValue.fromS(userId)))
                .build(),
        )
        logger.info("User $userId deregistered from server $serverId")
    }
}
