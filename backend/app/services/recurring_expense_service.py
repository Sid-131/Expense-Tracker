import calendar
import json
import uuid
from datetime import date, timedelta

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recurring_expense import RecurringExpense
from app.schemas.expense import ExpenseCreate, SplitInput, SplitTypeEnum
from app.schemas.recurring_expense import RecurringExpenseCreate
from app.services.expense_service import _require_member, _resolve_name, create_expense


def _advance_date(d: date, frequency: str) -> date:
    if frequency == "DAILY":
        return d + timedelta(days=1)
    elif frequency == "WEEKLY":
        return d + timedelta(weeks=1)
    elif frequency == "MONTHLY":
        month = d.month % 12 + 1
        year = d.year + (1 if d.month == 12 else 0)
        day = min(d.day, calendar.monthrange(year, month)[1])
        return d.replace(year=year, month=month, day=day)
    elif frequency == "YEARLY":
        return d.replace(year=d.year + 1)
    return d


async def create_recurring(
    db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID, data: RecurringExpenseCreate
) -> dict:
    await _require_member(db, user_id, group_id)

    splits_json = json.dumps([
        {k: str(v) if v is not None else None for k, v in s.model_dump().items()}
        for s in data.splits
    ])

    r = RecurringExpense(
        group_id=group_id,
        title=data.title,
        amount=data.amount,
        category=data.category,
        paid_by_user_id=data.paid_by_user_id,
        paid_by_guest_id=data.paid_by_guest_id,
        split_type=data.split_type,
        splits_json=splits_json,
        frequency=data.frequency,
        next_due_date=data.start_date,
        created_by=user_id,
    )
    db.add(r)
    await db.commit()
    await db.refresh(r)

    # If start_date is today or in the past, fire immediately
    if r.next_due_date <= date.today():
        await process_due(db)
        await db.refresh(r)

    return await _enrich(db, r)


async def list_recurring(
    db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID
) -> list[dict]:
    await _require_member(db, user_id, group_id)
    result = await db.execute(
        select(RecurringExpense)
        .where(RecurringExpense.group_id == group_id, RecurringExpense.is_active == True)
        .order_by(RecurringExpense.next_due_date)
    )
    return [await _enrich(db, r) for r in result.scalars().all()]


async def deactivate_recurring(
    db: AsyncSession, user_id: uuid.UUID, recurring_id: uuid.UUID
) -> None:
    result = await db.execute(
        select(RecurringExpense).where(RecurringExpense.id == recurring_id)
    )
    r = result.scalar_one_or_none()
    if not r:
        raise HTTPException(status_code=404, detail="Recurring expense not found")
    await _require_member(db, user_id, r.group_id)
    r.is_active = False
    await db.commit()


async def process_due(db: AsyncSession) -> int:
    """Create actual expenses for all due recurring templates. Returns count created."""
    today = date.today()
    result = await db.execute(
        select(RecurringExpense).where(
            RecurringExpense.is_active == True,
            RecurringExpense.next_due_date <= today,
        )
    )
    due = result.scalars().all()
    created = 0

    for r in due:
        try:
            raw_splits = json.loads(r.splits_json or "[]")
            splits = [
                SplitInput(
                    user_id=s.get("user_id"),
                    guest_id=s.get("guest_id"),
                    amount=s.get("amount"),
                    percentage=s.get("percentage"),
                )
                for s in raw_splits
            ]
            data = ExpenseCreate(
                title=r.title,
                amount=r.amount,
                category=r.category,
                paid_by_user_id=r.paid_by_user_id,
                paid_by_guest_id=r.paid_by_guest_id,
                split_type=SplitTypeEnum(r.split_type),
                splits=splits,
            )
            await create_expense(db, r.created_by, r.group_id, data)
            created += 1

            # advance next_due_date past today (catches up if missed multiple periods)
            while r.next_due_date <= today:
                r.next_due_date = _advance_date(r.next_due_date, r.frequency)

            await db.commit()
        except Exception:
            await db.rollback()

    return created


async def _enrich(db: AsyncSession, r: RecurringExpense) -> dict:
    return {
        "id": r.id,
        "group_id": r.group_id,
        "title": r.title,
        "amount": r.amount,
        "category": r.category,
        "paid_by_user_id": r.paid_by_user_id,
        "paid_by_guest_id": r.paid_by_guest_id,
        "paid_by_name": await _resolve_name(db, r.paid_by_user_id, r.paid_by_guest_id),
        "split_type": r.split_type,
        "frequency": r.frequency,
        "next_due_date": r.next_due_date,
        "is_active": r.is_active,
        "created_at": r.created_at,
    }
