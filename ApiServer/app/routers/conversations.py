import httpx

from fastapi import APIRouter, Depends, HTTPException

from app.config import settings
from app.dependencies import get_current_user
from app.models.user import User

router = APIRouter(prefix="/conversations", tags=["conversations"])


@router.get("/{conversation_id}/messages")
async def get_messages(
    conversation_id: str,
    _: User = Depends(get_current_user),
):
    async with httpx.AsyncClient() as client:
        resp = await client.get(
            f"{settings.chat_server_url}/conversations/{conversation_id}/messages",
            headers={"x-internal-key": settings.internal_api_key},
        )
    if not resp.is_success:
        raise HTTPException(status_code=502, detail="Failed to fetch messages")
    return resp.json()
