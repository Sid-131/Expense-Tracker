import uuid
from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, field_validator

from app.core.categories import normalize


class PersonalExpenseCreate(BaseModel):
    title: str
    amount: Decimal
    category: str = "OTHER"
    spent_at: Optional[date] = None
    note: Optional[str] = None
    source: str = "telegram"

    @field_validator("title")
    @classmethod
    def title_not_blank(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Title cannot be blank")
        return v.strip()

    @field_validator("amount")
    @classmethod
    def amount_positive(cls, v: Decimal) -> Decimal:
        if v <= 0:
            raise ValueError("Amount must be positive")
        return v

    @field_validator("category")
    @classmethod
    def category_canonical(cls, v: str) -> str:
        return normalize(v)


class PersonalExpenseResponse(BaseModel):
    id: uuid.UUID
    user_id: uuid.UUID
    title: str
    amount: Decimal
    category: str
    spent_at: date
    note: Optional[str]
    source: str
    created_at: datetime

    model_config = {"from_attributes": True}
