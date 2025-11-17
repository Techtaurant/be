from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.common.api.v1.router import api_router
from app.configs.config import settings
from app.configs.database import close_db
from app.configs.exception_handler import register_exception_handlers
from app.configs.redis import redis_client


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    애플리케이션 생명주기 관리

    시작 시 Redis 연결을 초기화하고,
    종료 시 데이터베이스와 Redis 연결을 종료합니다.
    """
    # Startup
    await redis_client.connect()
    yield
    # Shutdown
    await close_db()
    await redis_client.disconnect()


def create_app() -> FastAPI:
    """
    FastAPI 애플리케이션 인스턴스 생성

    Returns:
        FastAPI: 설정이 완료된 FastAPI 인스턴스
    """
    # FastAPI 애플리케이션 생성
    app = FastAPI(
        title=settings.APP_NAME,
        version=settings.APP_VERSION,
        docs_url="/swagger-ui/index.html",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )

    # CORS 설정
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.CORS_ORIGINS,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 전역 예외 핸들러 등록
    register_exception_handlers(app)

    # API 라우터 등록
    app.include_router(
        api_router,
    )

    # Health Check 엔드포인트
    _register_health_check(app)

    return app


def _register_health_check(app: FastAPI) -> None:
    """
    Health Check 엔드포인트 등록

    Args:
        app: FastAPI 인스턴스
    """

    @app.get("/health", tags=["Health"])
    async def health_check():
        """
        헬스 체크 엔드포인트

        서버가 정상적으로 동작하는지 확인합니다.

        Returns:
            dict: 상태 정보
        """
        from app.common.base_response import success_response

        return success_response(
            message="서버가 정상적으로 동작 중입니다",
            data={
                "app_name": settings.APP_NAME,
                "version": settings.APP_VERSION,
            },
        )
