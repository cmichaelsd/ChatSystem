from fastapi import FastAPI

from app.routers import presence

app = FastAPI()
app.include_router(presence.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
