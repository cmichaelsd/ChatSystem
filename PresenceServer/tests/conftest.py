import pytest_asyncio
from fakeredis.aioredis import FakeRedis
from httpx import AsyncClient, ASGITransport

from main import app
from app.dependencies import get_current_user_id, verify_internal_key
from app.redis import get_redis


def _make_redis_override(redis):
    async def override():
        return redis
    return override


@pytest_asyncio.fixture
async def fake_redis():
    redis = FakeRedis()
    yield redis
    await redis.aclose()


@pytest_asyncio.fixture
async def client(fake_redis):
    async def override_get_current_user_id():
        return "test-user-id"

    async def override_verify_internal_key():
        return None

    app.dependency_overrides[get_redis] = _make_redis_override(fake_redis)
    app.dependency_overrides[get_current_user_id] = override_get_current_user_id
    app.dependency_overrides[verify_internal_key] = override_verify_internal_key

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()


@pytest_asyncio.fixture
async def unauthed_client(fake_redis):
    app.dependency_overrides[get_redis] = _make_redis_override(fake_redis)

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac

    app.dependency_overrides.clear()
