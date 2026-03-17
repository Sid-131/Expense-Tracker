import enum
import uuid
from datetime import datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel


class SplitTypeEnum(str, enum.Enum):
    EQUAL = "EQUAL"
    PERCENTAGE = "PERCENTAGE"
    EXACT = "EXACT"


class SplitInput(BaseModel):
    user_id: Optional[uuid.UUID] = None
    guest_id: Optional[uuid.UUID] = None
    amount: Optional[Decimal] = None
    percentage: Optional[Decimal] = None


class ExpenseCreate(BaseModel):
    title: str
    amount: Decimal
    category: str = "OTHER"
    paid_by_user_id: Optional[uuid.UUID] = None
    paid_by_guest_id: Optional[uuid.UUID] = None
    split_type: SplitTypeEnum = SplitTypeEnum.EQUAL
    splits: list[SplitInput] = []


class ExpenseSplitResponse(BaseModel):
    id: uuid.UUID
    user_id: Optional[uuid.UUID]
    guest_id: Optional[uuid.UUID]
    member_name: str
    amount: Decimal
    percentage: Optional[Decimal]


class ExpenseResponse(BaseModel):
    id: uuid.UUID
    group_id: uuid.UUID
    title: str
    amount: Decimal
    category: str
    paid_by_user_id: Optional[uuid.UUID]
    paid_by_guest_id: Optional[uuid.UUID]
    paid_by_name: str
    split_type: str
    created_at: datetime

    model_config = {"from_attributes": True}


class ExpenseDetailResponse(ExpenseResponse):
    splits: list[ExpenseSplitResponse]


class BalanceResponse(BaseModel):
    user_id: Optional[uuid.UUID]
    guest_id: Optional[uuid.UUID]
    member_name: str
    net_amount: Decimal
