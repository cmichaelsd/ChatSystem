import pytest
from unittest.mock import AsyncMock, patch

from tests.conftest import register_and_login


@pytest.fixture
def mock_chat_server_messages():
    messages = [
        {"fromUserId": "alice-id", "conversationId": "conv-1", "content": "hello", "sentAt": "2024-01-01T00:00:00Z"},
        {"fromUserId": "bob-id", "conversationId": "conv-1", "content": "hi", "sentAt": "2024-01-01T00:00:01Z"},
    ]
    with patch("app.routers.conversations.get_conversation_messages", new_callable=AsyncMock) as mock:
        mock.return_value = messages
        yield messages


async def test_get_messages(client, mock_chat_server_messages):
    headers = await register_and_login(client, "alice", "alice@test.com")
    resp = await client.get("/conversations/conv-1/messages", headers=headers)
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 2
    assert data[0]["content"] == "hello"


async def test_get_messages_unauthenticated(client):
    resp = await client.get("/conversations/conv-1/messages")
    assert resp.status_code == 401
