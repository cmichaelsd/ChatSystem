package org.chatserver.registry

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

class ConversationRegistryTest {
    private val dynamoClient = mockk<DynamoDbClient>(relaxed = true)
    private val waiter = mockk<DynamoDbWaiter>(relaxed = true)
    private val registry = ConversationRegistry(dynamoClient)

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
    fun `addMember writes conversationId and userId to DynamoDB`() {
        registry.addMember("conv-1", "alice")

        verify { dynamoClient.putItem(any<PutItemRequest>()) }
    }

    @Test
    fun `getMembers returns list of user IDs for a conversation`() {
        val response =
            QueryResponse.builder()
                .items(
                    mapOf(
                        "conversationId" to AttributeValue.fromS("conv-1"),
                        "userId" to AttributeValue.fromS("alice"),
                    ),
                    mapOf(
                        "conversationId" to AttributeValue.fromS("conv-1"),
                        "userId" to AttributeValue.fromS("bob"),
                    ),
                )
                .build()
        every { dynamoClient.query(any<QueryRequest>()) } returns response

        val members = registry.getMembers("conv-1")

        assertEquals(listOf("alice", "bob"), members)
    }

    @Test
    fun `getMembers returns empty list when conversation has no members`() {
        val response = QueryResponse.builder().items(emptyList()).build()
        every { dynamoClient.query(any<QueryRequest>()) } returns response

        val members = registry.getMembers("empty-conv")

        assertTrue(members.isEmpty())
    }
}
