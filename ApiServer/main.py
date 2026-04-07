from fastapi import FastAPI

from app.routers import auth, users, groups

app = FastAPI(title="ChatSystem API")

app.include_router(auth.router)
app.include_router(users.router)
app.include_router(groups.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
