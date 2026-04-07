from fastapi import APIRouter, Depends
from redis.asyncio import Redis

from app.dependencies import get_current_user_id, verify_internal_key
from app.redis import get_redis
from app.repos.presence import set_online, is_online
from app.schemas.presence import HeartbeatRequest, PresenceResponse

router = APIRouter(prefix="/presence", tags=["presence"])


@router.post("/heartbeat", status_code=204)
async def heartbeat(
    payload: HeartbeatRequest,
    redis: Redis = Depends(get_redis),
    _: None = Depends(verify_internal_key),
):
    for user_id in payload.user_ids:
        await set_online(user_id, redis)


@router.get("/{user_id}", response_model=PresenceResponse)
async def get_presence(
    user_id: str,
    redis: Redis = Depends(get_redis),
    _: str = Depends(get_current_user_id),
):
    online = await is_online(user_id, redis)
    return PresenceResponse(user_id=user_id, online=online)
