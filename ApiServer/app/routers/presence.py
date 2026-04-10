import httpx

from fastapi import APIRouter, Depends, HTTPException

from app.config import settings
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.presence import BatchPresenceRequest

router = APIRouter(prefix="/presence", tags=["presence"])


@router.post("/batch")
async def batch_presence(
    payload: BatchPresenceRequest,
    _: User = Depends(get_current_user),
):
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            f"{settings.chat_server_url}/presence/batch",
            json={"user_ids": payload.user_ids},
            headers={"x-internal-key": settings.internal_api_key},
        )
    if not resp.is_success:
        raise HTTPException(status_code=502, detail="Failed to fetch presence")
    return resp.json()
