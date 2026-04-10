from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    secret_key: str
    chat_server_url: str
    internal_api_key: str
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30
    cors_origins: str = "http://localhost:5173"

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    model_config = {"env_file": ".env"}


settings = Settings()
