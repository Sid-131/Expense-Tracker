import logging
from contextlib import asynccontextmanager

from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from redis.asyncio import Redis

from app.api.v1 import auth
from app.api.v1.router import router as v1_router
from app.config import settings
from app.database import AsyncSessionLocal
from app.services import recurring_expense_service

logger = logging.getLogger(__name__)


async def _process_due_recurring():
    async with AsyncSessionLocal() as db:
        count = await recurring_expense_service.process_due(db)
        if count:
            logger.info("Recurring: created %d expense(s)", count)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    redis = Redis.from_url(settings.redis_url, decode_responses=False)
    app.state.redis = redis
    auth.get_redis = lambda: redis

    scheduler = AsyncIOScheduler()
    # Run daily at midnight, and once at startup to catch any missed ones
    scheduler.add_job(_process_due_recurring, CronTrigger(hour=0, minute=0),
                      id="recurring_daily", replace_existing=True)
    scheduler.start()
    # Run once immediately on startup (catches missed periods after downtime)
    await _process_due_recurring()

    yield

    # Shutdown
    scheduler.shutdown(wait=False)
    await redis.aclose()


app = FastAPI(
    title="Expensio API",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(v1_router, prefix="/api/v1")


@app.get("/health")
async def health():
    return {"status": "ok"}
