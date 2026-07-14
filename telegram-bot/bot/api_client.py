"""Thin async client over the Expensio API. Holds no DB; caches per-user JWT in Redis."""
from __future__ import annotations

from datetime import date
from decimal import Decimal

import httpx
from redis.asyncio import Redis

from bot.config import config

_redis = Redis.from_url(config.redis_url, decode_responses=True)
_http = httpx.AsyncClient(base_url=config.api_base, timeout=15.0)


def _jwt_key(telegram_id: int) -> str:
    return f"bot:jwt:{telegram_id}"


async def ensure_jwt(telegram_id: int, name: str, username: str | None) -> str:
    """Return a cached JWT, or mint one via the service-token endpoint and cache it."""
    cached = await _redis.get(_jwt_key(telegram_id))
    if cached:
        return cached

    resp = await _http.post(
        "/auth/telegram",
        headers={"X-Service-Token": config.service_token},
        json={"telegram_id": telegram_id, "name": name, "username": username},
    )
    resp.raise_for_status()
    token = resp.json()["access_token"]
    await _redis.set(_jwt_key(telegram_id), token, ex=config.jwt_cache_ttl)
    return token


async def _auth_headers(telegram_id: int, name: str, username: str | None) -> dict:
    jwt = await ensure_jwt(telegram_id, name, username)
    return {"Authorization": f"Bearer {jwt}"}


async def create_personal_expense(
    telegram_id: int, name: str, username: str | None,
    title: str, amount: Decimal, category: str, spent_at: date | None,
) -> dict:
    headers = await _auth_headers(telegram_id, name, username)
    body = {"title": title, "amount": str(amount), "category": category, "source": "telegram"}
    if spent_at:
        body["spent_at"] = spent_at.isoformat()
    resp = await _http.post("/personal-expenses", headers=headers, json=body)
    resp.raise_for_status()
    return resp.json()


async def delete_personal_expense(
    telegram_id: int, name: str, username: str | None, expense_id: str,
) -> bool:
    headers = await _auth_headers(telegram_id, name, username)
    resp = await _http.delete(f"/personal-expenses/{expense_id}", headers=headers)
    return resp.status_code == 204


async def aclose() -> None:
    await _http.aclose()
    await _redis.aclose()
