import uuid

from fastapi import HTTPException, status
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.group import Group, GroupMember, Guest
from app.models.user import User
from app.schemas.group import GroupCreate, AddMemberRequest


async def create_group(db: AsyncSession, user_id: uuid.UUID, data: GroupCreate) -> dict:
    group = Group(name=data.name, created_by=user_id)
    db.add(group)
    await db.flush()

    member = GroupMember(group_id=group.id, user_id=user_id)
    db.add(member)
    await db.commit()
    await db.refresh(group)
    return {**group.__dict__, "member_count": 1}


async def get_user_groups(db: AsyncSession, user_id: uuid.UUID) -> list[dict]:
    result = await db.execute(
        select(Group)
        .join(GroupMember, GroupMember.group_id == Group.id)
        .where(GroupMember.user_id == user_id)
        .order_by(Group.created_at.desc())
    )
    groups = result.scalars().all()

    output = []
    for g in groups:
        count_result = await db.execute(
            select(func.count()).where(GroupMember.group_id == g.id)
        )
        count = count_result.scalar() or 0
        output.append({**g.__dict__, "member_count": count})
    return output


async def get_group_detail(db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID) -> dict:
    await _require_member(db, user_id, group_id)

    result = await db.execute(select(Group).where(Group.id == group_id))
    group = result.scalar_one_or_none()
    if not group:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Group not found")

    members_result = await db.execute(
        select(GroupMember).where(GroupMember.group_id == group_id)
    )
    raw_members = members_result.scalars().all()

    member_list = []
    for m in raw_members:
        if m.user_id:
            user_result = await db.execute(select(User).where(User.id == m.user_id))
            user = user_result.scalar_one_or_none()
            if user:
                member_list.append({
                    "id": m.id,
                    "name": user.name,
                    "is_guest": False,
                    "user_id": user.id,
                    "guest_id": None,
                    "profile_pic": user.profile_pic,
                })
        elif m.guest_id:
            guest_result = await db.execute(select(Guest).where(Guest.id == m.guest_id))
            guest = guest_result.scalar_one_or_none()
            if guest:
                member_list.append({
                    "id": m.id,
                    "name": guest.name,
                    "is_guest": True,
                    "user_id": None,
                    "guest_id": guest.id,
                    "profile_pic": None,
                })

    return {
        "id": group.id,
        "name": group.name,
        "created_by": group.created_by,
        "created_at": group.created_at,
        "members": member_list,
    }


async def add_member(
    db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID, data: AddMemberRequest
) -> dict:
    await _require_member(db, user_id, group_id)

    if data.email:
        result = await db.execute(select(User).where(User.email == data.email))
        target = result.scalar_one_or_none()
        if not target:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

        existing = await db.execute(
            select(GroupMember).where(
                GroupMember.group_id == group_id,
                GroupMember.user_id == target.id,
            )
        )
        if existing.scalar_one_or_none():
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="User already in group")

        member = GroupMember(group_id=group_id, user_id=target.id)
        db.add(member)
        await db.commit()
        await db.refresh(member)
        return {"id": member.id, "name": target.name, "is_guest": False,
                "user_id": target.id, "guest_id": None, "profile_pic": target.profile_pic}

    elif data.guest_name:
        guest = Guest(name=data.guest_name, created_by=user_id)
        db.add(guest)
        await db.flush()

        member = GroupMember(group_id=group_id, guest_id=guest.id)
        db.add(member)
        await db.commit()
        await db.refresh(member)
        return {"id": member.id, "name": guest.name, "is_guest": True,
                "user_id": None, "guest_id": guest.id, "profile_pic": None}

    else:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST,
                            detail="Provide email or guest_name")


async def remove_member(
    db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID, member_id: uuid.UUID
) -> None:
    await _require_member(db, user_id, group_id)

    result = await db.execute(
        select(GroupMember).where(
            GroupMember.id == member_id,
            GroupMember.group_id == group_id,
        )
    )
    member = result.scalar_one_or_none()
    if not member:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Member not found")

    await db.delete(member)
    await db.commit()


async def _require_member(db: AsyncSession, user_id: uuid.UUID, group_id: uuid.UUID) -> None:
    result = await db.execute(
        select(GroupMember).where(
            GroupMember.group_id == group_id,
            GroupMember.user_id == user_id,
        )
    )
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN,
                            detail="Not a member of this group")
