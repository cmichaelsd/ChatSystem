from pydantic import BaseModel


class HeartbeatRequest(BaseModel):
    user_ids: list[str]


class PresenceResponse(BaseModel):
    user_id: str
    online: bool
