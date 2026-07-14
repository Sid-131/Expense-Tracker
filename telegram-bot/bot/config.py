import os


class Config:
    bot_token: str = os.environ["TELEGRAM_BOT_TOKEN"]
    service_token: str = os.environ["SERVICE_TOKEN"]
    api_base: str = os.environ.get("API_BASE", "http://backend:8000/api/v1")
    redis_url: str = os.environ.get("REDIS_URL", "redis://redis:6379")
    # cache the JWT just under the backend's 24h access-token lifetime
    jwt_cache_ttl: int = int(os.environ.get("JWT_CACHE_TTL", 23 * 3600))


config = Config()
