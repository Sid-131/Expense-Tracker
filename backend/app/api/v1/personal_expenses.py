import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_current_user
from app.models.user import User
from app.schemas.personal_expense import PersonalExpenseCreate, PersonalExpenseResponse
from app.services import personal_expense_service

router = APIRouter()


@router.post("", response_model=PersonalExpenseResponse, status_code=status.HTTP_201_CREATED)
async def create_personal_expense(
    body: PersonalExpenseCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    expense = await personal_expense_service.create(db, current_user.id, body)
    return expense


@router.get("", response_model=list[PersonalExpenseResponse])
async def list_personal_expenses(
    limit: int = 50,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return await personal_expense_service.list_for_user(db, current_user.id, limit)


@router.delete("/{expense_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_personal_expense(
    expense_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    deleted = await personal_expense_service.delete(db, current_user.id, expense_id)
    if not deleted:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Expense not found")
