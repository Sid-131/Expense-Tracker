import uuid
from datetime import datetime

from sqlalchemy import Boolean, Column, Date, DateTime, ForeignKey, Numeric, String, Text, func
from sqlalchemy.dialects.postgresql import UUID

from app.database import Base


class RecurringExpense(Base):
    __tablename__ = "recurring_expenses"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    group_id = Column(UUID(as_uuid=True), ForeignKey("groups.id", ondelete="CASCADE"), nullable=False)
    title = Column(String(200), nullable=False)
    amount = Column(Numeric(12, 2), nullable=False)
    category = Column(String(50), nullable=False)
    paid_by_user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    paid_by_guest_id = Column(UUID(as_uuid=True), ForeignKey("guests.id"), nullable=True)
    split_type = Column(String(20), nullable=False, default="EQUAL")
    splits_json = Column(Text, nullable=False, default="[]")
    frequency = Column(String(20), nullable=False)  # DAILY/WEEKLY/MONTHLY/YEARLY
    next_due_date = Column(Date, nullable=False)
    is_active = Column(Boolean, nullable=False, default=True)
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
