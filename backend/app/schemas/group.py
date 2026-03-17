import uuid
from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class GroupCreate(BaseModel):
    name: str


class MemberResponse(BaseModel):
    id: uuid.UUID
    name: str
    is_guest: bool
    user_id: Optional[uuid.UUID] = None
    guest_id: Optional[uuid.UUID] = None
    profile_pic: Optional[str] = None

    model_config = {"from_attributes": True}


class GroupResponse(BaseModel):
    id: uuid.UUID
    name: str
    created_by: Optional[uuid.UUID]
    created_at: datetime
    member_count: int

    model_config = {"from_attributes": True}


class GroupDetailResponse(BaseModel):
    id: uuid.UUID
    name: str
    created_by: Optional[uuid.UUID]
    created_at: datetime
    members: list[MemberResponse]

    model_config = {"from_attributes": True}


class AddMemberRequest(BaseModel):
    email: Optional[str] = None
    guest_name: Optional[str] = None
