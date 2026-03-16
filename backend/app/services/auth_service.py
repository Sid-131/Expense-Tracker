import logging
import random
import string
import uuid

from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token
from jose import JWTError
from redis.asyncio import Redis
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import settings
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)
from app.models.user import User
from app.schemas.auth import TokenResponse

logger = logging.getLogger(__name__)

OTP_TTL_SECONDS = 300  # 5 minutes


def _make_tokens(user_id: str) -> TokenResponse:
    return TokenResponse(
        access_token=create_access_token(str(user_id)),
        refresh_token=create_refresh_token(str(user_id)),
    )


async def signup_with_email(
    db: AsyncSession, name: str, email: str, password: str
) -> TokenResponse:
    result = await db.execute(select(User).where(User.email == email))
    if result.scalar_one_or_none():
        raise ValueError("Email already registered")

    user = User(
        id=uuid.uuid4(),
        name=name,
        email=email,
        password_hash=hash_password(password),
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return _make_tokens(str(user.id))


async def login_with_email(
    db: AsyncSession, email: str, password: str
) -> TokenResponse:
    result = await db.execute(select(User).where(User.email == email))
    user = result.scalar_one_or_none()

    if not user or not user.password_hash or not verify_password(password, user.password_hash):
        raise ValueError("Invalid email or password")

    return _make_tokens(str(user.id))


async def login_with_google(db: AsyncSession, id_token: str) -> TokenResponse:
    try:
        idinfo = google_id_token.verify_oauth2_token(
            id_token,
            google_requests.Request(),
            settings.google_client_id,
        )
    except Exception as e:
        raise ValueError(f"Invalid Google token: {e}")

    google_id = idinfo["sub"]
    email = idinfo.get("email")
    name = idinfo.get("name", "User")
    picture = idinfo.get("picture")

    # Try to find existing user by google_id first, then by email
    result = await db.execute(select(User).where(User.google_id == google_id))
    user = result.scalar_one_or_none()

    if not user and email:
        result = await db.execute(select(User).where(User.email == email))
        user = result.scalar_one_or_none()
        if user:
            user.google_id = google_id  # Link Google to existing email account

    if not user:
        user = User(
            id=uuid.uuid4(),
            name=name,
            email=email,
            google_id=google_id,
            profile_pic=picture,
        )
        db.add(user)

    await db.commit()
    await db.refresh(user)
    return _make_tokens(str(user.id))


async def send_otp(redis: Redis, phone: str) -> None:
    otp = "".join(random.choices(string.digits, k=6))
    await redis.setex(f"otp:{phone}", OTP_TTL_SECONDS, otp)
    # TODO: Send via Twilio/MSG91 in production
    logger.info("OTP for %s: %s", phone, otp)


async def verify_otp(db: AsyncSession, redis: Redis, phone: str, otp: str) -> TokenResponse:
    stored = await redis.get(f"otp:{phone}")
    if stored is None:
        raise ValueError("OTP expired or not sent")
    if stored.decode() != otp:
        raise ValueError("Invalid OTP")

    await redis.delete(f"otp:{phone}")

    result = await db.execute(select(User).where(User.phone == phone))
    user = result.scalar_one_or_none()

    if not user:
        user = User(id=uuid.uuid4(), name="User", phone=phone)
        db.add(user)
        await db.commit()
        await db.refresh(user)

    return _make_tokens(str(user.id))


async def refresh_tokens(refresh_token: str) -> TokenResponse:
    try:
        payload = decode_token(refresh_token)
        if payload.get("type") != "refresh":
            raise ValueError("Not a refresh token")
        user_id = payload["sub"]
    except JWTError:
        raise ValueError("Invalid or expired refresh token")

    return _make_tokens(user_id)
