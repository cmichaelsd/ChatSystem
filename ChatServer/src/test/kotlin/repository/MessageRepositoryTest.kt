package org.chatserver.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageRepositoryTest {
    private val dynamoClient = mockk<DynamoDbClient>(relaxed = true)
    private val waiter = mockk<DynamoDbWaiter>(relaxed = true)
    private val repository = MessageRepository(dynamoClient)

    @Test
    fun `init does not create table when it already exists`() {
        repository.init()

        verify(exactly = 0) { dynamoClient.createTable(any<CreateTableRequest>()) }
    }

    @Test
    fun `init creates table when it does not exist`() {
        every { dynamoClient.describeTable(any<Consumer<DescribeTableRequest.Builder>>()) } throws
            ResourceNotFoundException.builder().message("Table not found").build()
        every { dynamoClient.waiter() } returns waiter

        repository.init()

        verify { dynamoClient.createTable(any<CreateTableRequest>()) }
        verify { waiter.waitUntilTableExists(any<Consumer<DescribeTableRequest.Builder>>()) }
    }

    @Test
    fun `save writes message to DynamoDB`() {
        repository.save("alice", "conv-1", "hello")

        verify { dynamoClient.putItem(any<PutItemRequest>()) }
    }

    @Test
    fun `getMessages maps DynamoDB items to StoredMessage list`() {
        val response =
            QueryResponse.builder()
                .items(
                    mapOf(
                        "conversationId" to AttributeValue.fromS("conv-1"),
                        "sentAt" to AttributeValue.fromS("2024-01-01T00:00:00Z"),
                        "fromUserId" to AttributeValue.fromS("alice"),
                        "content" to AttributeValue.fromS("hello"),
                    ),
                    mapOf(
                        "conversationId" to AttributeValue.fromS("conv-1"),
                        "sentAt" to AttributeValue.fromS("2024-01-01T00:00:01Z"),
                        "fromUserId" to AttributeValue.fromS("bob"),
                        "content" to AttributeValue.fromS("world"),
                    ),
                )
                .build()
        every { dynamoClient.query(any<QueryRequest>()) } returns response

        val messages = repository.getMessages("conv-1")

        assertEquals(2, messages.size)
        assertEquals("alice", messages[0].fromUserId)
        assertEquals("conv-1", messages[0].conversationId)
        assertEquals("hello", messages[0].content)
        assertEquals("2024-01-01T00:00:00Z", messages[0].sentAt)
        assertEquals("bob", messages[1].fromUserId)
        assertEquals("world", messages[1].content)
    }

    @Test
    fun `getMessages returns empty list when no messages exist`() {
        val response = QueryResponse.builder().items(emptyList()).build()
        every { dynamoClient.query(any<QueryRequest>()) } returns response

        val messages = repository.getMessages("empty-conv")

        assertTrue(messages.isEmpty())
    }
}
