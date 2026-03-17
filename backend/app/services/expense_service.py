import uuid
from decimal import Decimal, ROUND_HALF_UP
from typing import Optional

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.expense import Balance, Expense, ExpenseSplit
from app.models.group import GroupMember, Guest
from app.models.user import User
from app.schemas.expense import ExpenseCreate, SplitInput, SplitTypeEnum


def _dec(value) -> Decimal:
    return Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


async def _require_member(db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID) -> None:
    result = await db.execute(
        select(GroupMember).where(
            GroupMember.group_id == group_id, GroupMember.user_id == user_id
        )
    )
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN,
                            detail="Not a member of this group")


async def _get_members(db: AsyncSession, group_id: uuid.UUID) -> list[dict]:
    result = await db.execute(
        select(GroupMember).where(GroupMember.group_id == group_id)
    )
    rows = result.scalars().all()
    members = []
    for m in rows:
        if m.user_id:
            u = (await db.execute(select(User).where(User.id == m.user_id))).scalar_one_or_none()
            if u:
                members.append({"user_id": u.id, "guest_id": None, "name": u.name})
        elif m.guest_id:
            g = (await db.execute(select(Guest).where(Guest.id == m.guest_id))).scalar_one_or_none()
            if g:
                members.append({"user_id": None, "guest_id": g.id, "name": g.name})
    return members


async def _resolve_name(db: AsyncSession, user_id, guest_id) -> str:
    if user_id:
        u = (await db.execute(select(User).where(User.id == user_id))).scalar_one_or_none()
        return u.name if u else "Unknown"
    if guest_id:
        g = (await db.execute(select(Guest).where(Guest.id == guest_id))).scalar_one_or_none()
        return g.name if g else "Unknown"
    return "Unknown"


def _equal_splits(members: list[dict], total: Decimal) -> list[dict]:
    n = len(members)
    total_cents = int(total * 100)
    base_cents, rem = divmod(total_cents, n)
    return [
        {
            "user_id": m["user_id"],
            "guest_id": m["guest_id"],
            "name": m["name"],
            "amount": Decimal(base_cents + (1 if i < rem else 0)) / 100,
            "percentage": None,
        }
        for i, m in enumerate(members)
    ]


def _percentage_splits(members: list[dict], total: Decimal, inputs: list[SplitInput]) -> list[dict]:
    total_pct = sum(_dec(s.percentage) for s in inputs if s.percentage is not None)
    if abs(total_pct - Decimal("100")) > Decimal("0.1"):
        raise HTTPException(status_code=400, detail=f"Percentages must sum to 100, got {total_pct}")

    pct_map = {str(s.user_id or s.guest_id): _dec(s.percentage) for s in inputs}
    splits, assigned = [], Decimal("0")
    for i, m in enumerate(members):
        key = str(m["user_id"] or m["guest_id"])
        pct = pct_map.get(key, Decimal("0"))
        amount = (total - assigned) if i == len(members) - 1 else (total * pct / 100).quantize(Decimal("0.01"))
        assigned += amount
        splits.append({"user_id": m["user_id"], "guest_id": m["guest_id"],
                        "name": m["name"], "amount": amount, "percentage": pct})
    return splits


def _exact_splits(members: list[dict], total: Decimal, inputs: list[SplitInput]) -> list[dict]:
    total_split = sum(_dec(s.amount) for s in inputs if s.amount is not None)
    if abs(total_split - total) > Decimal("0.01"):
        raise HTTPException(status_code=400,
                            detail=f"Split amounts ({total_split}) must equal expense amount ({total})")
    amount_map = {str(s.user_id or s.guest_id): _dec(s.amount) for s in inputs}
    return [
        {
            "user_id": m["user_id"], "guest_id": m["guest_id"], "name": m["name"],
            "amount": amount_map.get(str(m["user_id"] or m["guest_id"]), Decimal("0")),
            "percentage": None,
        }
        for m in members
    ]


async def _update_balance(db: AsyncSession, group_id: uuid.UUID,
                          user_id: Optional[uuid.UUID], guest_id: Optional[uuid.UUID],
                          delta: Decimal) -> None:
    if user_id:
        result = await db.execute(
            select(Balance).where(Balance.group_id == group_id, Balance.user_id == user_id)
        )
    else:
        result = await db.execute(
            select(Balance).where(Balance.group_id == group_id, Balance.guest_id == guest_id)
        )
    balance = result.scalar_one_or_none()
    if balance:
        balance.net_amount += delta
    else:
        db.add(Balance(group_id=group_id, user_id=user_id, guest_id=guest_id, net_amount=delta))


async def _apply_balances(db: AsyncSession, group_id: uuid.UUID,
                          paid_by_user_id, paid_by_guest_id,
                          splits: list[dict], mult: int = 1) -> None:
    total = sum(s["amount"] for s in splits)
    payer_split = next(
        (s["amount"] for s in splits
         if (paid_by_user_id and s["user_id"] == paid_by_user_id) or
            (paid_by_guest_id and s["guest_id"] == paid_by_guest_id)),
        Decimal("0"),
    )
    await _update_balance(db, group_id, paid_by_user_id, paid_by_guest_id,
                          Decimal(mult) * (total - payer_split))
    for s in splits:
        is_payer = (paid_by_user_id and s["user_id"] == paid_by_user_id) or \
                   (paid_by_guest_id and s["guest_id"] == paid_by_guest_id)
        if not is_payer:
            await _update_balance(db, group_id, s["user_id"], s["guest_id"],
                                  Decimal(mult) * (-s["amount"]))


async def create_expense(db: AsyncSession, user_id: uuid.UUID,
                         group_id: uuid.UUID, data: ExpenseCreate) -> dict:
    await _require_member(db, user_id, group_id)
    members = await _get_members(db, group_id)
    total = _dec(data.amount)

    if data.split_type == SplitTypeEnum.EQUAL:
        splits = _equal_splits(members, total)
    elif data.split_type == SplitTypeEnum.PERCENTAGE:
        if not data.splits:
            raise HTTPException(status_code=400, detail="splits required for PERCENTAGE")
        splits = _percentage_splits(members, total, data.splits)
    else:
        if not data.splits:
            raise HTTPException(status_code=400, detail="splits required for EXACT")
        splits = _exact_splits(members, total, data.splits)

    paid_by_name = await _resolve_name(db, data.paid_by_user_id, data.paid_by_guest_id)

    expense = Expense(
        group_id=group_id, title=data.title, amount=total, category=data.category,
        paid_by_user_id=data.paid_by_user_id, paid_by_guest_id=data.paid_by_guest_id,
        split_type=data.split_type, created_by=user_id,
    )
    db.add(expense)
    await db.flush()

    for s in splits:
        db.add(ExpenseSplit(expense_id=expense.id, user_id=s["user_id"], guest_id=s["guest_id"],
                            amount=s["amount"], percentage=s.get("percentage")))

    await _apply_balances(db, group_id, data.paid_by_user_id, data.paid_by_guest_id, splits)
    await db.commit()
    await db.refresh(expense)
    return {**expense.__dict__, "paid_by_name": paid_by_name}


async def get_group_expenses(db: AsyncSession, user_id: uuid.UUID,
                             group_id: uuid.UUID, skip: int = 0, limit: int = 20) -> list[dict]:
    await _require_member(db, user_id, group_id)
    result = await db.execute(
        select(Expense).where(Expense.group_id == group_id)
        .order_by(Expense.created_at.desc()).offset(skip).limit(limit)
    )
    return [
        {**e.__dict__, "paid_by_name": await _resolve_name(db, e.paid_by_user_id, e.paid_by_guest_id)}
        for e in result.scalars().all()
    ]


async def get_expense_detail(db: AsyncSession, user_id: uuid.UUID, expense_id: uuid.UUID) -> dict:
    expense = (await db.execute(select(Expense).where(Expense.id == expense_id))).scalar_one_or_none()
    if not expense:
        raise HTTPException(status_code=404, detail="Expense not found")
    await _require_member(db, user_id, expense.group_id)

    splits = (await db.execute(
        select(ExpenseSplit).where(ExpenseSplit.expense_id == expense_id)
    )).scalars().all()

    split_list = [
        {"id": s.id, "user_id": s.user_id, "guest_id": s.guest_id,
         "member_name": await _resolve_name(db, s.user_id, s.guest_id),
         "amount": s.amount, "percentage": s.percentage}
        for s in splits
    ]
    paid_by_name = await _resolve_name(db, expense.paid_by_user_id, expense.paid_by_guest_id)
    return {**expense.__dict__, "paid_by_name": paid_by_name, "splits": split_list}


async def update_expense(db: AsyncSession, user_id: uuid.UUID,
                         expense_id: uuid.UUID, data: ExpenseCreate) -> dict:
    expense = (await db.execute(select(Expense).where(Expense.id == expense_id))).scalar_one_or_none()
    if not expense:
        raise HTTPException(status_code=404, detail="Expense not found")
    await _require_member(db, user_id, expense.group_id)

    old_splits_rows = (await db.execute(
        select(ExpenseSplit).where(ExpenseSplit.expense_id == expense_id)
    )).scalars().all()
    old_splits = [{"user_id": s.user_id, "guest_id": s.guest_id, "amount": s.amount}
                  for s in old_splits_rows]

    await _apply_balances(db, expense.group_id,
                          expense.paid_by_user_id, expense.paid_by_guest_id,
                          old_splits, mult=-1)

    for s in old_splits_rows:
        await db.delete(s)

    members = await _get_members(db, expense.group_id)
    total = _dec(data.amount)
    if data.split_type == SplitTypeEnum.EQUAL:
        new_splits = _equal_splits(members, total)
    elif data.split_type == SplitTypeEnum.PERCENTAGE:
        new_splits = _percentage_splits(members, total, data.splits)
    else:
        new_splits = _exact_splits(members, total, data.splits)

    for s in new_splits:
        db.add(ExpenseSplit(expense_id=expense.id, user_id=s["user_id"], guest_id=s["guest_id"],
                            amount=s["amount"], percentage=s.get("percentage")))

    await _apply_balances(db, expense.group_id,
                          data.paid_by_user_id, data.paid_by_guest_id, new_splits)

    expense.title = data.title
    expense.amount = total
    expense.category = data.category
    expense.paid_by_user_id = data.paid_by_user_id
    expense.paid_by_guest_id = data.paid_by_guest_id
    expense.split_type = data.split_type

    await db.commit()
    await db.refresh(expense)
    paid_by_name = await _resolve_name(db, expense.paid_by_user_id, expense.paid_by_guest_id)
    return {**expense.__dict__, "paid_by_name": paid_by_name}


async def delete_expense(db: AsyncSession, user_id: uuid.UUID, expense_id: uuid.UUID) -> None:
    expense = (await db.execute(select(Expense).where(Expense.id == expense_id))).scalar_one_or_none()
    if not expense:
        raise HTTPException(status_code=404, detail="Expense not found")
    await _require_member(db, user_id, expense.group_id)

    splits = (await db.execute(
        select(ExpenseSplit).where(ExpenseSplit.expense_id == expense_id)
    )).scalars().all()
    splits_dicts = [{"user_id": s.user_id, "guest_id": s.guest_id, "amount": s.amount}
                    for s in splits]
    await _apply_balances(db, expense.group_id,
                          expense.paid_by_user_id, expense.paid_by_guest_id,
                          splits_dicts, mult=-1)
    await db.delete(expense)
    await db.commit()


async def get_group_balances(db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID) -> list[dict]:
    await _require_member(db, user_id, group_id)
    result = await db.execute(select(Balance).where(Balance.group_id == group_id))
    return [
        {"user_id": b.user_id, "guest_id": b.guest_id,
         "member_name": await _resolve_name(db, b.user_id, b.guest_id),
         "net_amount": b.net_amount}
        for b in result.scalars().all()
    ]
