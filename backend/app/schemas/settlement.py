import uuid
from datetime import datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel


class SettlementCreate(BaseModel):
    from_user_id: Optional[uuid.UUID] = None
    from_guest_id: Optional[uuid.UUID] = None
    to_user_id: Optional[uuid.UUID] = None
    to_guest_id: Optional[uuid.UUID] = None
    amount: Decimal


class SettlementSuggestion(BaseModel):
    from_user_id: Optional[uuid.UUID]
    from_guest_id: Optional[uuid.UUID]
    from_name: str
    to_user_id: Optional[uuid.UUID]
    to_guest_id: Optional[uuid.UUID]
    to_name: str
    amount: Decimal


class SettlementResponse(BaseModel):
    id: uuid.UUID
    group_id: uuid.UUID
    from_user_id: Optional[uuid.UUID]
    from_guest_id: Optional[uuid.UUID]
    from_name: str
    to_user_id: Optional[uuid.UUID]
    to_guest_id: Optional[uuid.UUID]
    to_name: str
    amount: Decimal
    status: str
    created_at: datetime
    completed_at: Optional[datetime]
