from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    redis_url: str = "redis://redis:6379"
    jwt_secret: str
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 1440   # 24 hours
    refresh_token_expire_days: int = 30
    debug: bool = False
    google_client_id: str = ""

    @property
    def async_database_url(self) -> str:
        """
        Convert any postgres:// or postgresql:// URL to the asyncpg driver format.
        Neon gives:  postgresql://user:pass@host/db?sslmode=require
        We need:     postgresql+asyncpg://user:pass@host/db?ssl=require
        """
        url = self.database_url
        # Replace driver prefix
        url = url.replace("postgresql://", "postgresql+asyncpg://", 1)
        url = url.replace("postgres://", "postgresql+asyncpg://", 1)
        # Neon uses ?sslmode=require — asyncpg uses ?ssl=require
        url = url.replace("sslmode=require", "ssl=require")
        return url

    class Config:
        env_file = ".env"


settings = Settings()
