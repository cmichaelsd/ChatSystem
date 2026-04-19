package org.chatserver.data

import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

abstract class AbstractDynamoCompositeRegistry(protected val dynamoClient: DynamoDbClient) {
    protected abstract val tableName: String
    protected abstract val partitionKey: String
    protected abstract val sortKey: String

    protected open fun gsiDefinitions(): List<GlobalSecondaryIndex> = emptyList()

    protected open fun gsiAttributeDefinitions(): List<AttributeDefinition> = emptyList()

    private val logger = LoggerFactory.getLogger(AbstractDynamoCompositeRegistry::class.java)

    protected fun ensureTableExists() {
        try {
            dynamoClient.describeTable { it.tableName(tableName) }
        } catch (e: ResourceNotFoundException) {
            logger.info("$tableName table not found, creating...")
            try {
                val attrDefs =
                    buildList {
                        add(AttributeDefinition.builder().attributeName(partitionKey).attributeType(ScalarAttributeType.S).build())
                        add(AttributeDefinition.builder().attributeName(sortKey).attributeType(ScalarAttributeType.S).build())
                        addAll(gsiAttributeDefinitions())
                    }
                val tableBuilder =
                    CreateTableRequest.builder()
                        .tableName(tableName)
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .attributeDefinitions(attrDefs)
                        .keySchema(
                            KeySchemaElement.builder().attributeName(partitionKey).keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName(sortKey).keyType(KeyType.RANGE).build(),
                        )
                val gsis = gsiDefinitions()
                if (gsis.isNotEmpty()) tableBuilder.globalSecondaryIndexes(gsis)
                dynamoClient.createTable(tableBuilder.build())
                dynamoClient.waiter().waitUntilTableExists { it.tableName(tableName) }
                logger.info("$tableName table created")
            } catch (e: ResourceInUseException) {
                logger.info("$tableName table already exists, skipping creation")
            }
        }
    }
}
