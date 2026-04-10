import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, delete

from app.chat_client import create_conversation, add_conversation_member, remove_conversation_member
from app.dependencies import get_db, get_current_user
from app.models.user import User
from app.models.group import Group, GroupMember
from app.schemas.group import GroupCreate, GroupResponse, GroupMemberResponse
from app.schemas.user import UserResponse

router = APIRouter(prefix="/groups", tags=["groups"])


@router.get("", response_model=list[GroupResponse])
async def list_my_groups(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(
        select(Group).join(GroupMember, Group.id == GroupMember.group_id).where(GroupMember.user_id == current_user.id)
    )
    return result.scalars().all()


@router.post("", response_model=GroupResponse, status_code=status.HTTP_201_CREATED)
async def create_group(
    payload: GroupCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    group = Group(id=str(uuid.uuid4()), name=payload.name, created_by=current_user.id)
    db.add(group)
    db.add(GroupMember(group_id=group.id, user_id=current_user.id))
    await db.commit()
    await db.refresh(group)
    await create_conversation(group.id, [current_user.id])
    return group


@router.get("/{group_id}", response_model=GroupResponse)
async def get_group(
    group_id: str,
    db: AsyncSession = Depends(get_db),
    _: User = Depends(get_current_user),
):
    result = await db.execute(select(Group).where(Group.id == group_id))
    group = result.scalar_one_or_none()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    return group


@router.delete("/{group_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_group(
    group_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Group).where(Group.id == group_id))
    group = result.scalar_one_or_none()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    if group.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="Only the group owner can delete it")
    await db.execute(delete(GroupMember).where(GroupMember.group_id == group_id))
    await db.delete(group)
    await db.commit()


@router.post("/{group_id}/members/{user_id}", response_model=GroupMemberResponse, status_code=status.HTTP_201_CREATED)
async def add_member(
    group_id: str,
    user_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Group).where(Group.id == group_id))
    group = result.scalar_one_or_none()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    if group.created_by != current_user.id:
        raise HTTPException(status_code=403, detail="Only the group owner can add members")

    result = await db.execute(select(User).where(User.id == user_id))
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="User not found")

    result = await db.execute(
        select(GroupMember).where(GroupMember.group_id == group_id, GroupMember.user_id == user_id)
    )
    if result.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="User is already a member")

    member = GroupMember(group_id=group_id, user_id=user_id)
    db.add(member)
    await db.commit()
    await db.refresh(member)
    await add_conversation_member(group_id, user_id)
    return member


@router.delete("/{group_id}/members/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_member(
    group_id: str,
    user_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    result = await db.execute(select(Group).where(Group.id == group_id))
    group = result.scalar_one_or_none()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    if group.created_by != current_user.id and current_user.id != user_id:
        raise HTTPException(status_code=403, detail="Not authorized")

    result = await db.execute(
        select(GroupMember).where(GroupMember.group_id == group_id, GroupMember.user_id == user_id)
    )
    member = result.scalar_one_or_none()
    if not member:
        raise HTTPException(status_code=404, detail="Member not found")

    await db.delete(member)
    await db.commit()
    await remove_conversation_member(group_id, user_id)


@router.get("/{group_id}/members", response_model=list[UserResponse])
async def list_members(
    group_id: str,
    db: AsyncSession = Depends(get_db),
    _: User = Depends(get_current_user),
):
    result = await db.execute(select(Group).where(Group.id == group_id))
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Group not found")

    result = await db.execute(
        select(User).join(GroupMember, User.id == GroupMember.user_id).where(GroupMember.group_id == group_id)
    )
    return result.scalars().all()
