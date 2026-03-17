from decimal import Decimal
from pydantic import BaseModel


class CategorySpend(BaseModel):
    category: str
    amount: Decimal
    count: int
    percentage: float


class MonthlySpend(BaseModel):
    month: str  # "2026-01"
    amount: Decimal


class AnalyticsResponse(BaseModel):
    total_spent: Decimal
    this_month: Decimal
    last_month: Decimal
    net_balance: Decimal
    group_count: int
    by_category: list[CategorySpend]
    by_month: list[MonthlySpend]
