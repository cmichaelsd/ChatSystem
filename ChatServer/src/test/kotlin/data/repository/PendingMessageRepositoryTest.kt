package org.chatserver.data.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.chatserver.models.ChatMessage
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest
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

class PendingMessageRepositoryTest {
    private val dynamoClient = mockk<DynamoDbClient>(relaxed = true)
    private val waiter = mockk<DynamoDbWaiter>(relaxed = true)
    private val repository = PendingMessageRepository(dynamoClient)

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
    fun `save writes pending message to DynamoDB`() {
        val message = ChatMessage(fromUserId = "alice", toUserId = "bob", conversationId = "conv-1", content = "hello")
        repository.save(message)

        verify { dynamoClient.putItem(any<PutItemRequest>()) }
    }

    @Test
    fun `fetchAndClear returns mapped ChatMessages`() {
        val response =
            QueryResponse.builder()
                .items(
                    mapOf(
                        "userId" to AttributeValue.fromS("bob"),
                        "messageId" to AttributeValue.fromS("msg-uuid-1"),
                        "fromUserId" to AttributeValue.fromS("alice"),
                        "conversationId" to AttributeValue.fromS("conv-1"),
                        "content" to AttributeValue.fromS("hello"),
                    ),
                )
                .build()
        every { dynamoClient.query(any<QueryRequest>()) } returns response

        val messages = repository.fetchAndClear("bob")

        assertEquals(1, messages.size)
        assertEquals(ChatMessage(fromUserId = "alice", toUserId = "bob", conversationId = "conv-1", content = "hello"), messages[0])
    }

    @Test
    fun `fetchAndClear deletes each item from DynamoDB after reading`() {
        val response =
            QueryResponse.builder()
                .items(
                    mapOf(
                        "userId" to AttributeValue.fromS("bob"),
                        "messageId" to AttributeValue.fromS("msg-1"),
                        "fromUserId" to AttributeValue.fromS("alice"),
                        "conversationId" to AttributeValue.fromS("conv-1"),
                        "content" to AttributeValue.fromS("hello"),
                    ),
                    mapOf(
                        "userId" to AttributeValue.fromS("bob"),
                        "messageId" to AttributeValue.fromS("msg-2"),
                        "fromUserId" to AttributeValue.fromS("alice"),
                        "conversationId" to AttributeValue.fromS("conv-1"),
                        "content" to AttributeValue.fromS("world"),
                    ),
                )
                .build()
        every { dynamoClient.query(any<QueryRequest>()) } returns response

        repository.fetchAndClear("bob")

        verify(exactly = 1) { dynamoClient.batchWriteItem(any<BatchWriteItemRequest>()) }
    }

    @Test
    fun `fetchAndClear returns empty list when no pending messages exist`() {
        val response = QueryResponse.builder().items(emptyList()).build()
        every { dynamoClient.query(any<QueryRequest>()) } returns response

        val messages = repository.fetchAndClear("alice")

        assertTrue(messages.isEmpty())
        verify(exactly = 0) { dynamoClient.batchWriteItem(any<BatchWriteItemRequest>()) }
    }
}
