import logging

from aws_xray_sdk.core import xray_recorder, patch
from fastapi import FastAPI, Request

from app.routers import presence

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)

patch(["redis"])

app = FastAPI()
app.include_router(presence.router)


@app.middleware("http")
async def xray_middleware(request: Request, call_next):
    xray_recorder.begin_segment(f"{request.method} {request.url.path}")
    try:
        return await call_next(request)
    finally:
        xray_recorder.end_segment()


@app.get("/health")
async def health():
    return {"status": "ok"}
