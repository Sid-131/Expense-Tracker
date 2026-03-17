from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_current_user, get_db
from app.models.user import User
from app.schemas.analytics import AnalyticsResponse
from app.schemas.user import UserResponse, UserSearchResponse
from app.services.analytics_service import get_analytics

router = APIRouter()


@router.get("/me", response_model=UserResponse)
async def get_me(current_user: User = Depends(get_current_user)):
    return current_user


@router.get("/search", response_model=list[UserSearchResponse])
async def search_users(
    q: str = Query(min_length=1),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(User)
        .where(User.email.ilike(f"%{q}%"), User.id != current_user.id)
        .limit(10)
    )
    return result.scalars().all()


@router.get("/analytics", response_model=AnalyticsResponse)
async def get_user_analytics(
    range: str = Query(default="3m", pattern="^(3m|6m|1y|all)$"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    months_map = {"3m": 3, "6m": 6, "1y": 12, "all": 24}
    months = months_map[range]
    return await get_analytics(db, current_user.id, months)
