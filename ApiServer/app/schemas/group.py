from datetime import datetime

from pydantic import BaseModel


class GroupCreate(BaseModel):
    name: str


class GroupResponse(BaseModel):
    id: str
    name: str
    created_by: str
    created_at: datetime

    model_config = {"from_attributes": True}


class GroupMemberResponse(BaseModel):
    group_id: str
    user_id: str
    joined_at: datetime

    model_config = {"from_attributes": True}
