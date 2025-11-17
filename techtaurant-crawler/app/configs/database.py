"""
데이터베이스 연결 관리

SQLAlchemy Async Engine과 Session을 생성하고 관리합니다.
"""

from collections.abc import AsyncGenerator

from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase

from app.configs.config import settings


class Base(DeclarativeBase):
    """
    모든 SQLAlchemy 모델의 기본 클래스

    모든 모델은 이 클래스를 상속받아 정의합니다.
    """

    pass


# Async Engine 생성
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=settings.DEBUG,  # SQL 쿼리 로깅 (디버그 모드에서만)
    pool_pre_ping=True,  # 연결 재사용 전 유효성 검증
    pool_size=settings.DB_POOL_SIZE,
    max_overflow=settings.DB_MAX_OVERFLOW,
)

# Session Factory
AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,  # commit 후에도 객체 사용 가능
    autoflush=False,
    autocommit=False,
)


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """
    데이터베이스 세션 의존성

    FastAPI의 Depends에서 사용되며, 요청마다 새로운 세션을 생성하고
    요청 종료 시 자동으로 세션을 닫습니다.

    Yields:
        AsyncSession: 데이터베이스 세션

    Example:
        ```python
        @router.get("/users")
        async def get_users(db: AsyncSession = Depends(get_db)):
            result = await db.execute(select(User))
            return result.scalars().all()
        ```
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()


async def init_db() -> None:
    """
    데이터베이스 테이블 초기화

    개발 환경에서만 사용하며, 프로덕션에서는 Alembic을 사용합니다.
    """
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


async def close_db() -> None:
    """
    데이터베이스 연결 종료

    애플리케이션 종료 시 호출됩니다.
    """
    await engine.dispose()
