import asyncio
import logging
import sys
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app.database import Base, engine
from app.routers import auth, users, groups, conversations
from app.config import settings
import app.models.user  # noqa: F401
import app.models.group  # noqa: F401

logging.basicConfig(
    level=logging.INFO,
    stream=sys.stdout,
    format="%(asctime)s %(levelname)-5s %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)

MAX_RETRIES = 10
RETRY_DELAY = 2


async def wait_for_db():
    for attempt in range(MAX_RETRIES):
        try:
            async with engine.connect() as conn:
                await conn.execute(text("SELECT 1"))
            logger.info("Database ready")
            return
        except Exception:
            logger.warning("DB not ready (attempt %d/%d)", attempt + 1, MAX_RETRIES)
            await asyncio.sleep(RETRY_DELAY)
    raise RuntimeError("Database never became ready")


@asynccontextmanager
async def lifespan(app: FastAPI):
    await wait_for_db()
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    logger.info("Tables ready")
    yield


app = FastAPI(title="ChatSystem API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(users.router)
app.include_router(groups.router)
app.include_router(conversations.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
