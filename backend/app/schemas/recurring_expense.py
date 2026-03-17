import uuid
from datetime import date, datetime
from decimal import Decimal
from enum import Enum
from typing import Optional

from pydantic import BaseModel

from app.schemas.expense import SplitInput


class FrequencyEnum(str, Enum):
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    MONTHLY = "MONTHLY"
    YEARLY = "YEARLY"


class RecurringExpenseCreate(BaseModel):
    title: str
    amount: Decimal
    category: str
    paid_by_user_id: Optional[uuid.UUID] = None
    paid_by_guest_id: Optional[uuid.UUID] = None
    split_type: str = "EQUAL"
    splits: list[SplitInput] = []
    frequency: FrequencyEnum
    start_date: date


class RecurringExpenseResponse(BaseModel):
    id: uuid.UUID
    group_id: uuid.UUID
    title: str
    amount: Decimal
    category: str
    paid_by_user_id: Optional[uuid.UUID]
    paid_by_guest_id: Optional[uuid.UUID]
    paid_by_name: str
    split_type: str
    frequency: str
    next_due_date: date
    is_active: bool
    created_at: datetime
