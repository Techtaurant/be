"""
애플리케이션 설정 관리

.env 파일에서 환경 변수를 로드하고 검증합니다.
"""

from typing import Any

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    애플리케이션 설정 클래스

    .env 파일에서 환경 변수를 로드하고 검증합니다.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # 애플리케이션
    APP_NAME: str = "DevDeb API"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False

    # 데이터베이스 (PostgreSQL)
    DATABASE_URL: str
    DB_POOL_SIZE: int = 5
    DB_MAX_OVERFLOW: int = 10

    # Redis
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0
    REDIS_PASSWORD: str | None = None

    # CORS
    CORS_ORIGINS: list[str] | str = ["*"]

    @field_validator("CORS_ORIGINS", mode="before")
    @classmethod
    def parse_cors_origins(cls, v: Any) -> list[str] | str:
        """
        CORS origins를 파싱합니다.

        문자열로 입력된 경우 JSON 리스트로 파싱합니다.
        """
        if isinstance(v, str) and not v.startswith("["):
            return [i.strip() for i in v.split(",")]
        elif isinstance(v, (list, str)):
            return v
        raise ValueError(v)


# 전역 설정 인스턴스
settings = Settings()
