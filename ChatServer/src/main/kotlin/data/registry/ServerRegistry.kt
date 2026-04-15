package org.chatserver.data.registry

import org.chatserver.data.AbstractDynamoRegistry
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import java.net.InetAddress
import java.time.Instant

class ServerRegistry(
    dynamoClient: DynamoDbClient,
    private val serverId: String,
) : AbstractDynamoRegistry(dynamoClient) {
    override val tableName = "ServerRegistry"
    override val partitionKey = "serverId"

    private val logger = LoggerFactory.getLogger(ServerRegistry::class.java)

    fun init() {
        ensureTableExists()
    }

    fun register() {
        val ip = runCatching { InetAddress.getLocalHost().hostAddress }.getOrDefault("unknown")

        dynamoClient.putItem(
            PutItemRequest.builder()
                .tableName(tableName)
                .item(
                    mapOf(
                        "serverId" to AttributeValue.fromS(serverId),
                        "queueUrl" to AttributeValue.fromS(""),
                        "ip" to AttributeValue.fromS(ip),
                        "registeredAt" to AttributeValue.fromS(Instant.now().toString()),
                    ),
                )
                .build(),
        )

        logger.info("Registered server $serverId (ip=$ip) in $tableName")
    }

    fun updateQueueUrl(queueUrl: String) {
        dynamoClient.updateItem(
            UpdateItemRequest.builder()
                .tableName(tableName)
                .key(mapOf("serverId" to AttributeValue.fromS(serverId)))
                .updateExpression("SET queueUrl = :url")
                .expressionAttributeValues(mapOf(":url" to AttributeValue.fromS(queueUrl)))
                .build(),
        )
        logger.info("Updated queueUrl for server $serverId")
    }

    fun getAllQueueUrls(): List<String> {
        val response =
            dynamoClient.scan {
                it.tableName(tableName)
                it.projectionExpression("queueUrl")
            }
        return response.items().mapNotNull { it["queueUrl"]?.s()?.takeIf { url -> url.isNotEmpty() } }
    }

    fun getQueueUrl(targetServerId: String): String? {
        val response =
            dynamoClient.getItem {
                it.tableName(tableName)
                it.key(mapOf("serverId" to AttributeValue.fromS(targetServerId)))
            }
        return response.item()["queueUrl"]?.s()?.takeIf { it.isNotEmpty() }
    }

    fun deregister() {
        dynamoClient.deleteItem {
            it.tableName(tableName)
            it.key(mapOf("serverId" to AttributeValue.fromS(serverId)))
        }
        logger.info("Deregistered server $serverId from $tableName")
    }
}
