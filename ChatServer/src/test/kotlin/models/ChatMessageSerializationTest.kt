package org.chatserver.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chatserver.models.ChatMessage
import org.chatserver.models.InboundMessage
import org.chatserver.models.StoredMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMessageSerializationTest {
    @Test
    fun `InboundMessage round-trips through JSON`() {
        val original = InboundMessage(conversationId = "conv-1", content = "hello world")
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<InboundMessage>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `ChatMessage round-trips through JSON`() {
        val original =
            ChatMessage(
                fromUserId = "alice",
                toUserId = "bob",
                conversationId = "conv-1",
                content = "hi there",
            )
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<ChatMessage>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `StoredMessage round-trips through JSON`() {
        val original =
            StoredMessage(
                fromUserId = "alice",
                conversationId = "conv-1",
                content = "stored message",
                sentAt = "2024-01-01T00:00:00Z",
            )
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<StoredMessage>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `ChatMessage JSON contains all expected fields`() {
        val message = ChatMessage("alice", "bob", "conv-1", "hello")
        val json = Json.encodeToString(message)
        assert(json.contains("\"fromUserId\":\"alice\""))
        assert(json.contains("\"toUserId\":\"bob\""))
        assert(json.contains("\"conversationId\":\"conv-1\""))
        assert(json.contains("\"content\":\"hello\""))
    }

    @Test
    fun `InboundMessage does not require fromUserId field`() {
        val json = """{"conversationId":"conv-1","content":"hello"}"""
        val decoded = Json.decodeFromString<InboundMessage>(json)
        assertEquals("conv-1", decoded.conversationId)
        assertEquals("hello", decoded.content)
    }
}
