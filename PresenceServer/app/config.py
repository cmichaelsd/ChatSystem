from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    redis_url: str
    secret_key: str
    internal_api_key: str
    algorithm: str = "HS256"
    heartbeat_ttl_seconds: int = 30
    model_config = {"env_file": ".env"}


settings = Settings()
