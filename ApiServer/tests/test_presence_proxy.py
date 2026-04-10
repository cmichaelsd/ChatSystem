from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from tests.conftest import register_and_login


@pytest.fixture
def mock_chat_server_presence():
    mock_response = MagicMock()
    mock_response.is_success = True
    mock_response.json.return_value = {"presence": {"user-1": True, "user-2": False}}

    mock_client = AsyncMock()
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)
    mock_client.post = AsyncMock(return_value=mock_response)

    with patch("app.routers.presence.httpx.AsyncClient", return_value=mock_client):
        yield mock_response.json.return_value


async def test_batch_presence(client, mock_chat_server_presence):
    headers = await register_and_login(client, "alice", "alice@test.com")
    resp = await client.post(
        "/presence/batch",
        json={"user_ids": ["user-1", "user-2"]},
        headers=headers,
    )
    assert resp.status_code == 200
    assert resp.json()["presence"]["user-1"] is True
    assert resp.json()["presence"]["user-2"] is False


async def test_batch_presence_unauthenticated(client):
    resp = await client.post("/presence/batch", json={"user_ids": ["user-1"]})
    assert resp.status_code == 401
