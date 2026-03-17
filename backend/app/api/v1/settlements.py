import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_current_user, get_db
from app.models.user import User
from app.schemas.settlement import SettlementResponse
from app.services import settlement_service

router = APIRouter()


@router.patch("/{settlement_id}/complete", response_model=SettlementResponse)
async def complete_settlement(
    settlement_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await settlement_service.complete_settlement(db, current_user.id, settlement_id)
