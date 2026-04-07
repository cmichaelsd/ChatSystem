from redis.asyncio import Redis

from app.config import settings


async def set_online(user_id: str, redis: Redis) -> None:
    await redis.set(f"presence:{user_id}", "1", ex=settings.heartbeat_ttl_seconds)


async def is_online(user_id: str, redis: Redis) -> bool:
    return await redis.exists(f"presence:{user_id}") == 1
