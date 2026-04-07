import pytest
from unittest.mock import AsyncMock, patch

from tests.conftest import register_and_login


@pytest.fixture(autouse=True)
def mock_chat_client():
    with patch("app.routers.groups.create_conversation", new_callable=AsyncMock), \
         patch("app.routers.groups.add_conversation_member", new_callable=AsyncMock), \
         patch("app.routers.groups.remove_conversation_member", new_callable=AsyncMock):
        yield


async def test_create_group(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    resp = await client.post("/groups", json={"name": "Test Group"}, headers=headers)
    assert resp.status_code == 201
    assert resp.json()["name"] == "Test Group"


async def test_get_group(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    group_id = (await client.post("/groups", json={"name": "Test Group"}, headers=headers)).json()["id"]
    resp = await client.get(f"/groups/{group_id}", headers=headers)
    assert resp.status_code == 200


async def test_get_group_not_found(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    resp = await client.get("/groups/00000000-0000-0000-0000-000000000000", headers=headers)
    assert resp.status_code == 404


async def test_delete_group(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    group_id = (await client.post("/groups", json={"name": "Test Group"}, headers=headers)).json()["id"]
    resp = await client.delete(f"/groups/{group_id}", headers=headers)
    assert resp.status_code == 204


async def test_delete_group_not_owner(client):
    owner_headers = await register_and_login(client, "alice", "alice@test.com")
    other_headers = await register_and_login(client, "bob", "bob@test.com")
    group_id = (await client.post("/groups", json={"name": "Test Group"}, headers=owner_headers)).json()["id"]
    resp = await client.delete(f"/groups/{group_id}", headers=other_headers)
    assert resp.status_code == 403


async def test_add_member(client):
    owner_headers = await register_and_login(client, "alice", "alice@test.com")
    bob_headers = await register_and_login(client, "bob", "bob@test.com")
    bob_id = (await client.get("/users/me", headers=bob_headers)).json()["id"]
    group_id = (await client.post("/groups", json={"name": "Test Group"}, headers=owner_headers)).json()["id"]
    resp = await client.post(f"/groups/{group_id}/members/{bob_id}", headers=owner_headers)
    assert resp.status_code == 201


async def test_add_member_already_member(client):
    owner_headers = await register_and_login(client, "alice", "alice@test.com")
    bob_headers = await register_and_login(client, "bob", "bob@test.com")
    bob_id = (await client.get("/users/me", headers=bob_headers)).json()["id"]
    group_id = (await client.post("/groups", json={"name": "Test Group"}, headers=owner_headers)).json()["id"]
    await client.post(f"/groups/{group_id}/members/{bob_id}", headers=owner_headers)
    resp = await client.post(f"/groups/{group_id}/members/{bob_id}", headers=owner_headers)
    assert resp.status_code == 400


async def test_remove_member(client):
    owner_headers = await register_and_login(client, "alice", "alice@test.com")
    bob_headers = await register_and_login(client, "bob", "bob@test.com")
    bob_id = (await client.get("/users/me", headers=bob_headers)).json()["id"]
    group_id = (await client.post("/groups", json={"name": "Test Group"}, headers=owner_headers)).json()["id"]
    await client.post(f"/groups/{group_id}/members/{bob_id}", headers=owner_headers)
    resp = await client.delete(f"/groups/{group_id}/members/{bob_id}", headers=owner_headers)
    assert resp.status_code == 204


async def test_list_members(client):
    owner_headers = await register_and_login(client, "alice", "alice@test.com")
    bob_headers = await register_and_login(client, "bob", "bob@test.com")
    bob_id = (await client.get("/users/me", headers=bob_headers)).json()["id"]
    group_id = (await client.post("/groups", json={"name": "Test Group"}, headers=owner_headers)).json()["id"]
    await client.post(f"/groups/{group_id}/members/{bob_id}", headers=owner_headers)
    resp = await client.get(f"/groups/{group_id}/members", headers=owner_headers)
    assert resp.status_code == 200
    assert len(resp.json()) == 2  # alice (owner) + bob
