package org.chatserver.registry

import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

abstract class DynamoCompositeRegistry(protected val dynamoClient: DynamoDbClient) {
    protected abstract val tableName: String
    protected abstract val partitionKey: String
    protected abstract val sortKey: String

    private val logger = LoggerFactory.getLogger(DynamoCompositeRegistry::class.java)

    protected fun ensureTableExists() {
        try {
            dynamoClient.describeTable { it.tableName(tableName) }
        } catch (e: ResourceNotFoundException) {
            logger.info("$tableName table not found, creating...")
            dynamoClient.createTable(
                CreateTableRequest.builder()
                    .tableName(tableName)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(partitionKey).attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(sortKey).attributeType(ScalarAttributeType.S).build(),
                    )
                    .keySchema(
                        KeySchemaElement.builder().attributeName(partitionKey).keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName(sortKey).keyType(KeyType.RANGE).build(),
                    )
                    .build(),
            )
            dynamoClient.waiter().waitUntilTableExists { it.tableName(tableName) }
            logger.info("$tableName table created")
        } catch (e: ResourceInUseException) {
            logger.info("$tableName table already exists, skipping creation")
        }
    }
}
