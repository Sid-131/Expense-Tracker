import uuid

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_current_user, get_db
from app.models.user import User
from app.schemas.group import (
    AddMemberRequest,
    GroupCreate,
    GroupDetailResponse,
    GroupResponse,
    MemberResponse,
)
from app.schemas.expense import ExpenseCreate, ExpenseResponse, BalanceResponse
from app.schemas.settlement import SettlementCreate, SettlementResponse, SettlementSuggestion
from app.schemas.recurring_expense import RecurringExpenseCreate, RecurringExpenseResponse
from app.services import group_service, expense_service, settlement_service, recurring_expense_service

router = APIRouter()


@router.post("", response_model=GroupResponse, status_code=201)
async def create_group(
    data: GroupCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await group_service.create_group(db, current_user.id, data)


@router.get("", response_model=list[GroupResponse])
async def list_groups(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await group_service.get_user_groups(db, current_user.id)


@router.get("/{group_id}", response_model=GroupDetailResponse)
async def get_group(
    group_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await group_service.get_group_detail(db, current_user.id, group_id)


@router.post("/{group_id}/members", response_model=MemberResponse, status_code=201)
async def add_member(
    group_id: uuid.UUID,
    data: AddMemberRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await group_service.add_member(db, current_user.id, group_id, data)


@router.delete("/{group_id}/members/{member_id}", status_code=204)
async def remove_member(
    group_id: uuid.UUID,
    member_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await group_service.remove_member(db, current_user.id, group_id, member_id)


@router.post("/{group_id}/expenses", response_model=ExpenseResponse, status_code=201)
async def create_expense(
    group_id: uuid.UUID,
    data: ExpenseCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await expense_service.create_expense(db, current_user.id, group_id, data)


@router.get("/{group_id}/expenses", response_model=list[ExpenseResponse])
async def list_expenses(
    group_id: uuid.UUID,
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=20, le=100),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await expense_service.get_group_expenses(db, current_user.id, group_id, skip, limit)


@router.get("/{group_id}/balances", response_model=list[BalanceResponse])
async def get_balances(
    group_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await expense_service.get_group_balances(db, current_user.id, group_id)


@router.get("/{group_id}/settlements/suggestions", response_model=list[SettlementSuggestion])
async def get_settlement_suggestions(
    group_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await settlement_service.get_suggestions(db, current_user.id, group_id)


@router.post("/{group_id}/settlements", response_model=SettlementResponse, status_code=201)
async def create_settlement(
    group_id: uuid.UUID,
    data: SettlementCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await settlement_service.create_settlement(db, current_user.id, group_id, data)


@router.get("/{group_id}/settlements", response_model=list[SettlementResponse])
async def list_settlements(
    group_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await settlement_service.get_group_settlements(db, current_user.id, group_id)


@router.post("/{group_id}/recurring", response_model=RecurringExpenseResponse, status_code=201)
async def create_recurring_expense(
    group_id: uuid.UUID,
    data: RecurringExpenseCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await recurring_expense_service.create_recurring(db, current_user.id, group_id, data)


@router.get("/{group_id}/recurring", response_model=list[RecurringExpenseResponse])
async def list_recurring_expenses(
    group_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await recurring_expense_service.list_recurring(db, current_user.id, group_id)


@router.delete("/{group_id}/recurring/{recurring_id}", status_code=204)
async def deactivate_recurring_expense(
    group_id: uuid.UUID,
    recurring_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await recurring_expense_service.deactivate_recurring(db, current_user.id, recurring_id)
