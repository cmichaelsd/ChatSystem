package org.chatserver.registry

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerRegistryTest {
    private val dynamoClient = mockk<DynamoDbClient>(relaxed = true)
    private val waiter = mockk<DynamoDbWaiter>(relaxed = true)
    private val registry = ServerRegistry(dynamoClient, serverId = "server-1")

    @Test
    fun `init does not create table when it already exists`() {
        registry.init()

        verify(exactly = 0) { dynamoClient.createTable(any<CreateTableRequest>()) }
    }

    @Test
    fun `init creates table when it does not exist`() {
        every { dynamoClient.describeTable(any<Consumer<DescribeTableRequest.Builder>>()) } throws
            ResourceNotFoundException.builder().message("Table not found").build()
        every { dynamoClient.waiter() } returns waiter

        registry.init()

        verify { dynamoClient.createTable(any<CreateTableRequest>()) }
        verify { waiter.waitUntilTableExists(any<Consumer<DescribeTableRequest.Builder>>()) }
    }

    @Test
    fun `register writes server entry to DynamoDB`() {
        registry.register()

        verify { dynamoClient.putItem(any<PutItemRequest>()) }
    }

    @Test
    fun `updateQueueUrl updates the queue URL attribute`() {
        registry.updateQueueUrl("https://sqs/queue-1")

        verify { dynamoClient.updateItem(any<UpdateItemRequest>()) }
    }

    @Test
    fun `getQueueUrl returns URL when server entry has a non-empty queue URL`() {
        val response =
            GetItemResponse.builder()
                .item(mapOf("queueUrl" to AttributeValue.fromS("https://sqs/queue-1")))
                .build()
        every { dynamoClient.getItem(any<Consumer<GetItemRequest.Builder>>()) } returns response

        assertEquals("https://sqs/queue-1", registry.getQueueUrl("server-2"))
    }

    @Test
    fun `getQueueUrl returns null when queue URL is empty string`() {
        val response =
            GetItemResponse.builder()
                .item(mapOf("queueUrl" to AttributeValue.fromS("")))
                .build()
        every { dynamoClient.getItem(any<Consumer<GetItemRequest.Builder>>()) } returns response

        assertNull(registry.getQueueUrl("server-2"))
    }

    @Test
    fun `getQueueUrl returns null when server entry does not exist`() {
        val response = GetItemResponse.builder().item(emptyMap()).build()
        every { dynamoClient.getItem(any<Consumer<GetItemRequest.Builder>>()) } returns response

        assertNull(registry.getQueueUrl("ghost-server"))
    }

    @Test
    fun `deregister removes server entry from DynamoDB`() {
        registry.deregister()

        verify { dynamoClient.deleteItem(any<Consumer<software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest.Builder>>()) }
    }
}
