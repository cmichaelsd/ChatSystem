import logging

from redis.asyncio import Redis

from app.config import settings

logger = logging.getLogger(__name__)


async def set_online(user_id: str, redis: Redis) -> None:
    logger.info("set_online user_id=%s ttl=%ds", user_id, settings.heartbeat_ttl_seconds)
    await redis.set(f"presence:{user_id}", "1", ex=settings.heartbeat_ttl_seconds)


async def is_online(user_id: str, redis: Redis) -> bool:
    return await redis.exists(f"presence:{user_id}") == 1


async def are_online(user_ids: list[str], redis: Redis) -> dict[str, bool]:
    if not user_ids:
        return {}
    keys = [f"presence:{uid}" for uid in user_ids]
    values = await redis.mget(*keys)
    return {uid: val is not None for uid, val in zip(user_ids, values)}
