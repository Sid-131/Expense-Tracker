import uuid
from datetime import date, timezone, datetime
from decimal import Decimal

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.expense import Balance, Expense, ExpenseSplit
from app.models.group import GroupMember
from app.schemas.analytics import AnalyticsResponse, CategorySpend, MonthlySpend


async def get_analytics(
    db: AsyncSession, user_id: uuid.UUID, months: int = 3
) -> AnalyticsResponse:
    # ── 1. Expenses where this user paid (their personal spend) ─────────────
    # We count what the user owes (their share in splits) across all groups
    split_q = (
        select(
            Expense.category,
            func.sum(ExpenseSplit.amount).label("total"),
            func.count(ExpenseSplit.id).label("cnt"),
            func.to_char(Expense.created_at, "YYYY-MM").label("month"),
        )
        .join(Expense, Expense.id == ExpenseSplit.expense_id)
        .where(ExpenseSplit.user_id == user_id)
        .group_by(Expense.category, func.to_char(Expense.created_at, "YYYY-MM"))
    )
    rows = (await db.execute(split_q)).all()

    # ── 2. Aggregate totals ─────────────────────────────────────────────────
    total_spent = sum((r.total or Decimal("0")) for r in rows)

    now = datetime.now(timezone.utc).date()
    this_m = now.strftime("%Y-%m")
    last_date = date(now.year if now.month > 1 else now.year - 1, (now.month - 2) % 12 + 1, 1)
    last_m = last_date.strftime("%Y-%m")

    this_month = sum((r.total or Decimal("0")) for r in rows if r.month == this_m)
    last_month = sum((r.total or Decimal("0")) for r in rows if r.month == last_m)

    # ── 3. By-category (all time) ───────────────────────────────────────────
    cat_totals: dict[str, Decimal] = {}
    cat_counts: dict[str, int] = {}
    for r in rows:
        cat = r.category or "OTHER"
        cat_totals[cat] = cat_totals.get(cat, Decimal("0")) + (r.total or Decimal("0"))
        cat_counts[cat] = cat_counts.get(cat, 0) + r.cnt

    by_category = []
    for cat, amt in sorted(cat_totals.items(), key=lambda x: x[1], reverse=True):
        pct = float(amt / total_spent * 100) if total_spent else 0.0
        by_category.append(
            CategorySpend(category=cat, amount=amt, count=cat_counts[cat], percentage=round(pct, 1))
        )

    # ── 4. By-month trend (last N months) ───────────────────────────────────
    month_totals: dict[str, Decimal] = {}
    for r in rows:
        month_totals[r.month] = month_totals.get(r.month, Decimal("0")) + (r.total or Decimal("0"))

    # Build ordered list for last `months` months
    ordered_months = []
    for i in range(months - 1, -1, -1):
        m_date = date(now.year if now.month > i else now.year - (1 + (i - now.month) // 12),
                      (now.month - i - 1) % 12 + 1, 1)
        m_str = m_date.strftime("%Y-%m")
        ordered_months.append(MonthlySpend(month=m_str, amount=month_totals.get(m_str, Decimal("0"))))

    # ── 5. Net balance (sum across all groups) ──────────────────────────────
    bal_q = select(func.sum(Balance.net_amount)).where(Balance.user_id == user_id)
    net_balance = (await db.execute(bal_q)).scalar() or Decimal("0")

    # ── 6. Group count ──────────────────────────────────────────────────────
    grp_q = select(func.count()).select_from(GroupMember).where(GroupMember.user_id == user_id)
    group_count = (await db.execute(grp_q)).scalar() or 0

    return AnalyticsResponse(
        total_spent=total_spent,
        this_month=this_month,
        last_month=last_month,
        net_balance=net_balance,
        group_count=group_count,
        by_category=by_category,
        by_month=ordered_months,
    )
