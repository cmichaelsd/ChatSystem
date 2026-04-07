async def test_register_success(client):
    resp = await client.post("/auth/register", json={"username": "alice", "email": "alice@test.com", "password": "password"})
    assert resp.status_code == 201
    data = resp.json()
    assert data["username"] == "alice"
    assert data["email"] == "alice@test.com"
    assert "id" in data


async def test_register_duplicate(client):
    payload = {"username": "alice", "email": "alice@test.com", "password": "password"}
    await client.post("/auth/register", json=payload)
    resp = await client.post("/auth/register", json=payload)
    assert resp.status_code == 400


async def test_login_success(client):
    await client.post("/auth/register", json={"username": "alice", "email": "alice@test.com", "password": "password"})
    resp = await client.post("/auth/login", data={"username": "alice", "password": "password"})
    assert resp.status_code == 200
    assert "access_token" in resp.json()


async def test_login_invalid_credentials(client):
    resp = await client.post("/auth/login", data={"username": "nobody", "password": "wrong"})
    assert resp.status_code == 401
