from fastapi import APIRouter, Depends

from app.chat_client import get_conversation_messages
from app.dependencies import get_current_user
from app.models.user import User

router = APIRouter(prefix="/conversations", tags=["conversations"])


@router.get("/{conversation_id}/messages")
async def get_messages(
    conversation_id: str,
    _: User = Depends(get_current_user),
):
    return await get_conversation_messages(conversation_id)
