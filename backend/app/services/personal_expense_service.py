import uuid
from datetime import date

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.personal_expense import PersonalExpense
from app.schemas.personal_expense import PersonalExpenseCreate


async def create(
    db: AsyncSession, user_id: uuid.UUID, body: PersonalExpenseCreate
) -> PersonalExpense:
    expense = PersonalExpense(
        id=uuid.uuid4(),
        user_id=user_id,
        title=body.title,
        amount=body.amount,
        category=body.category,
        spent_at=body.spent_at or date.today(),
        note=body.note,
        source=body.source,
    )
    db.add(expense)
    await db.commit()
    await db.refresh(expense)
    return expense


async def list_for_user(
    db: AsyncSession, user_id: uuid.UUID, limit: int = 50
) -> list[PersonalExpense]:
    result = await db.execute(
        select(PersonalExpense)
        .where(PersonalExpense.user_id == user_id)
        .order_by(PersonalExpense.spent_at.desc(), PersonalExpense.created_at.desc())
        .limit(limit)
    )
    return list(result.scalars().all())


async def delete(db: AsyncSession, user_id: uuid.UUID, expense_id: uuid.UUID) -> bool:
    result = await db.execute(
        select(PersonalExpense).where(
            PersonalExpense.id == expense_id,
            PersonalExpense.user_id == user_id,
        )
    )
    expense = result.scalar_one_or_none()
    if not expense:
        return False
    await db.delete(expense)
    await db.commit()
    return True
