import uuid
from datetime import datetime, timezone
from decimal import Decimal

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.expense import Balance
from app.models.group import GroupMember
from app.models.settlement import Settlement
from app.schemas.settlement import SettlementCreate
from app.services.expense_service import _require_member, _resolve_name, _update_balance


async def get_suggestions(db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID) -> list[dict]:
    await _require_member(db, user_id, group_id)

    result = await db.execute(select(Balance).where(Balance.group_id == group_id))
    balances = result.scalars().all()

    creditors = [(b.net_amount, b) for b in balances if b.net_amount > 0]
    debtors = [(-b.net_amount, b) for b in balances if b.net_amount < 0]
    creditors.sort(key=lambda x: x[0], reverse=True)
    debtors.sort(key=lambda x: x[0], reverse=True)

    suggestions = []
    i, j = 0, 0
    credit_amounts = [c[0] for c in creditors]
    debt_amounts = [d[0] for d in debtors]

    while i < len(creditors) and j < len(debtors):
        amount = min(credit_amounts[i], debt_amounts[j])
        creditor = creditors[i][1]
        debtor = debtors[j][1]

        suggestions.append({
            "from_user_id": debtor.user_id,
            "from_guest_id": debtor.guest_id,
            "from_name": await _resolve_name(db, debtor.user_id, debtor.guest_id),
            "to_user_id": creditor.user_id,
            "to_guest_id": creditor.guest_id,
            "to_name": await _resolve_name(db, creditor.user_id, creditor.guest_id),
            "amount": amount,
        })

        credit_amounts[i] -= amount
        debt_amounts[j] -= amount
        if credit_amounts[i] == 0:
            i += 1
        if debt_amounts[j] == 0:
            j += 1

    return suggestions


async def create_settlement(
    db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID, data: SettlementCreate
) -> dict:
    await _require_member(db, user_id, group_id)

    settlement = Settlement(
        group_id=group_id,
        from_user_id=data.from_user_id,
        from_guest_id=data.from_guest_id,
        to_user_id=data.to_user_id,
        to_guest_id=data.to_guest_id,
        amount=data.amount,
        status="PENDING",
        created_by=user_id,
    )
    db.add(settlement)
    await db.commit()
    await db.refresh(settlement)

    return await _enrich(db, settlement)


async def complete_settlement(
    db: AsyncSession, user_id: uuid.UUID, settlement_id: uuid.UUID
) -> dict:
    result = await db.execute(select(Settlement).where(Settlement.id == settlement_id))
    settlement = result.scalar_one_or_none()
    if not settlement:
        raise HTTPException(status_code=404, detail="Settlement not found")

    await _require_member(db, user_id, settlement.group_id)

    if settlement.status == "COMPLETED":
        raise HTTPException(status_code=400, detail="Settlement already completed")

    # Update balances: payer's balance goes up, receiver's goes down
    await _update_balance(db, settlement.group_id,
                          settlement.from_user_id, settlement.from_guest_id,
                          settlement.amount)
    await _update_balance(db, settlement.group_id,
                          settlement.to_user_id, settlement.to_guest_id,
                          -settlement.amount)

    settlement.status = "COMPLETED"
    settlement.completed_at = datetime.now(timezone.utc)
    await db.commit()
    await db.refresh(settlement)

    return await _enrich(db, settlement)


async def get_group_settlements(
    db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID
) -> list[dict]:
    await _require_member(db, user_id, group_id)
    result = await db.execute(
        select(Settlement)
        .where(Settlement.group_id == group_id)
        .order_by(Settlement.created_at.desc())
    )
    return [await _enrich(db, s) for s in result.scalars().all()]


async def _enrich(db: AsyncSession, s: Settlement) -> dict:
    return {
        "id": s.id,
        "group_id": s.group_id,
        "from_user_id": s.from_user_id,
        "from_guest_id": s.from_guest_id,
        "from_name": await _resolve_name(db, s.from_user_id, s.from_guest_id),
        "to_user_id": s.to_user_id,
        "to_guest_id": s.to_guest_id,
        "to_name": await _resolve_name(db, s.to_user_id, s.to_guest_id),
        "amount": s.amount,
        "status": s.status,
        "created_at": s.created_at,
        "completed_at": s.completed_at,
    }
