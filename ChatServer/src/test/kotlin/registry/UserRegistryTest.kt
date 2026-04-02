package org.chatserver.registry

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRegistryTest {
    private val dynamoClient = mockk<DynamoDbClient>(relaxed = true)
    private val waiter = mockk<DynamoDbWaiter>(relaxed = true)
    private val registry = UserRegistry(dynamoClient, serverId = "server-1")

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
    fun `init does not throw when table already exists on creation race`() {
        every { dynamoClient.describeTable(any<Consumer<DescribeTableRequest.Builder>>()) } throws
            ResourceNotFoundException.builder().message("Table not found").build()
        every { dynamoClient.createTable(any<CreateTableRequest>()) } throws
            ResourceInUseException.builder().message("Table already exists").build()

        registry.init()
    }

    @Test
    fun `register writes userId and serverId to DynamoDB`() {
        registry.register("alice")

        verify { dynamoClient.putItem(any<PutItemRequest>()) }
    }

    @Test
    fun `getServerId returns serverId when user is registered`() {
        val response =
            GetItemResponse.builder()
                .item(mapOf("serverId" to AttributeValue.fromS("server-2")))
                .build()
        every { dynamoClient.getItem(any<Consumer<GetItemRequest.Builder>>()) } returns response

        val result = registry.getServerId("alice")

        assertEquals("server-2", result)
    }

    @Test
    fun `getServerId returns null when user is not registered`() {
        val response = GetItemResponse.builder().item(emptyMap()).build()
        every { dynamoClient.getItem(any<Consumer<GetItemRequest.Builder>>()) } returns response

        val result = registry.getServerId("unknown")

        assertNull(result)
    }

    @Test
    fun `deregister removes user from DynamoDB`() {
        registry.deregister("alice")

        verify { dynamoClient.deleteItem(any<DeleteItemRequest>()) }
    }
}
