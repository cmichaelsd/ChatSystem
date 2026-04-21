import httpx
from fastapi import HTTPException

from app.config import settings


_INTERNAL_HEADERS = {"x-internal-key": settings.internal_api_key}


async def create_conversation(conversation_id: str, member_ids: list[str]):
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{settings.chat_server_url}/conversations",
            json={"conversationId": conversation_id, "memberIds": member_ids},
            headers=_INTERNAL_HEADERS,
        )
    if response.status_code != 201:
        raise HTTPException(status_code=502, detail="Failed to create conversation on chat server")


async def add_conversation_member(conversation_id: str, user_id: str):
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{settings.chat_server_url}/conversations/{conversation_id}/members/{user_id}",
            headers=_INTERNAL_HEADERS,
        )
    if response.status_code != 201:
        raise HTTPException(status_code=502, detail="Failed to add member on chat server")


async def remove_conversation_member(conversation_id: str, user_id: str):
    async with httpx.AsyncClient() as client:
        response = await client.delete(
            f"{settings.chat_server_url}/conversations/{conversation_id}/members/{user_id}",
            headers=_INTERNAL_HEADERS,
        )
    if response.status_code != 204:
        raise HTTPException(status_code=502, detail="Failed to remove member on chat server")


async def get_conversation_messages(conversation_id: str) -> list:
    async with httpx.AsyncClient() as client:
        response = await client.get(
            f"{settings.chat_server_url}/conversations/{conversation_id}/messages",
            headers=_INTERNAL_HEADERS,
        )
    if not response.is_success:
        raise HTTPException(status_code=502, detail="Failed to fetch messages")
    return response.json()
