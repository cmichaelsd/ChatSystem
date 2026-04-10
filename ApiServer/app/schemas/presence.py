from pydantic import BaseModel


class BatchPresenceRequest(BaseModel):
    user_ids: list[str]
