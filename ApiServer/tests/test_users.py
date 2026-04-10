from tests.conftest import register_and_login


async def test_get_me(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    resp = await client.get("/users/me", headers=headers)
    assert resp.status_code == 200
    assert resp.json()["username"] == "alice"


async def test_get_me_unauthenticated(client):
    resp = await client.get("/users/me")
    assert resp.status_code == 401


async def test_get_user_by_id(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    me = (await client.get("/users/me", headers=headers)).json()
    resp = await client.get(f"/users/{me['id']}", headers=headers)
    assert resp.status_code == 200
    assert resp.json()["id"] == me["id"]


async def test_get_user_not_found(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    resp = await client.get("/users/00000000-0000-0000-0000-000000000000", headers=headers)
    assert resp.status_code == 404


async def test_search_users_returns_matches(client):
    headers = await register_and_login(client, "alice", "alice@test.com")
    await register_and_login(client, "alicia", "alicia@test.com")
    await register_and_login(client, "bob", "bob@test.com")
    resp = await client.get("/users/search?username=ali", headers=headers)
    assert resp.status_code == 200
    usernames = [u["username"] for u in resp.json()]
    assert "alice" in usernames
    assert "alicia" in usernames
    assert "bob" not in usernames


async def test_search_users_unauthenticated(client):
    resp = await client.get("/users/search?username=alice")
    assert resp.status_code == 401
