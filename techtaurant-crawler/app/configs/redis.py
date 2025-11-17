"""
Redis 클라이언트 관리

Redis 연결 및 기본 CRUD 작업을 제공합니다.
"""

import json
from typing import Any

from redis.asyncio import Redis

from app.configs.config import settings
from app.configs.exceptions import RedisException


class RedisClient:
    """
    Redis 클라이언트 래퍼 클래스

    Redis 연결 및 기본 작업을 제공합니다.
    """

    def __init__(self) -> None:
        """Redis 클라이언트 초기화"""
        self._redis: Redis | None = None

    async def connect(self) -> None:
        """
        Redis 연결 초기화

        애플리케이션 시작 시 호출됩니다.
        """
        try:
            self._redis = await Redis(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                db=settings.REDIS_DB,
                password=settings.REDIS_PASSWORD,
                decode_responses=True,
                encoding="utf-8",
            )
            # 연결 테스트
            await self._redis.ping()
        except Exception as e:
            raise RedisException(f"Redis 연결 실패: {e}") from e

    async def disconnect(self) -> None:
        """
        Redis 연결 종료

        애플리케이션 종료 시 호출됩니다.
        """
        if self._redis:
            await self._redis.close()
            await self._redis.connection_pool.disconnect()

    def _ensure_connected(self) -> Redis:
        """Redis 연결 확인"""
        if self._redis is None:
            raise RedisException("Redis가 연결되지 않았습니다")
        return self._redis

    async def get(self, key: str) -> str | None:
        """
        키로 값 조회

        Args:
            key: Redis 키

        Returns:
            str | None: 값 (존재하지 않으면 None)
        """
        redis = self._ensure_connected()
        return await redis.get(key)

    async def set(
        self,
        key: str,
        value: str,
        ex: int | None = None,
    ) -> bool:
        """
        키-값 저장

        Args:
            key: Redis 키
            value: 저장할 값
            ex: 만료 시간 (초 단위, None이면 무제한)

        Returns:
            bool: 성공 여부
        """
        redis = self._ensure_connected()
        return await redis.set(key, value, ex=ex)

    async def delete(self, key: str) -> int:
        """
        키 삭제

        Args:
            key: Redis 키

        Returns:
            int: 삭제된 키의 개수
        """
        redis = self._ensure_connected()
        return await redis.delete(key)

    async def exists(self, key: str) -> bool:
        """
        키 존재 여부 확인

        Args:
            key: Redis 키

        Returns:
            bool: 존재 여부
        """
        redis = self._ensure_connected()
        return await redis.exists(key) > 0

    async def set_json(
        self,
        key: str,
        value: Any,
        ex: int | None = None,
    ) -> bool:
        """
        JSON 직렬화하여 저장

        Args:
            key: Redis 키
            value: 저장할 객체 (JSON 직렬화 가능)
            ex: 만료 시간 (초 단위)

        Returns:
            bool: 성공 여부
        """
        json_str = json.dumps(value, ensure_ascii=False)
        return await self.set(key, json_str, ex=ex)

    async def get_json(self, key: str) -> Any | None:
        """
        JSON 역직렬화하여 조회

        Args:
            key: Redis 키

        Returns:
            Any | None: 역직렬화된 객체 (존재하지 않으면 None)
        """
        value = await self.get(key)
        if value is None:
            return None
        return json.loads(value)


# 전역 Redis 클라이언트 인스턴스
redis_client = RedisClient()


async def get_redis() -> RedisClient:
    """
    Redis 클라이언트 의존성

    FastAPI의 Depends에서 사용됩니다.

    Returns:
        RedisClient: Redis 클라이언트

    Example:
        ```python
        @router.get("/cache")
        async def get_cache(redis: RedisClient = Depends(get_redis)):
            value = await redis.get("key")
            return {"value": value}
        ```
    """
    return redis_client
