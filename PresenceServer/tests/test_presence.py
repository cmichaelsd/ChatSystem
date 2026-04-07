async def test_heartbeat_sets_online(client, fake_redis):
    resp = await client.post("/presence/heartbeat", json={"user_ids": ["user-1", "user-2"]})
    assert resp.status_code == 204
    assert await fake_redis.exists("presence:user-1") == 1
    assert await fake_redis.exists("presence:user-2") == 1


async def test_heartbeat_key_has_ttl(client, fake_redis):
    await client.post("/presence/heartbeat", json={"user_ids": ["user-1"]})
    ttl = await fake_redis.ttl("presence:user-1")
    assert ttl > 0


async def test_heartbeat_wrong_key(unauthed_client):
    resp = await unauthed_client.post(
        "/presence/heartbeat",
        json={"user_ids": ["user-1"]},
        headers={"x-internal-key": "wrongkey"},
    )
    assert resp.status_code == 403


async def test_get_presence_online(client, fake_redis):
    await fake_redis.set("presence:user-1", "1", ex=30)
    resp = await client.get("/presence/user-1")
    assert resp.status_code == 200
    assert resp.json() == {"user_id": "user-1", "online": True}


async def test_get_presence_offline(client):
    resp = await client.get("/presence/user-1")
    assert resp.status_code == 200
    assert resp.json() == {"user_id": "user-1", "online": False}


async def test_get_presence_unauthenticated(unauthed_client):
    resp = await unauthed_client.get("/presence/user-1")
    assert resp.status_code == 401
