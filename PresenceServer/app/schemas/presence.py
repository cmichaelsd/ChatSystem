from pydantic import BaseModel


class HeartbeatRequest(BaseModel):
    user_ids: list[str]


class PresenceResponse(BaseModel):
    user_id: str
    online: bool


class BatchPresenceRequest(BaseModel):
    user_ids: list[str]


class BatchPresenceResponse(BaseModel):
    presence: dict[str, bool]
