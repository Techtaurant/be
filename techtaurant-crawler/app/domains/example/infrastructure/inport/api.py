"""
예제 API 엔드포인트

데이터베이스, Redis, 예외 처리 등을 테스트할 수 있는 예제 API입니다.
"""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.common.base_response import BaseResponse, success_response
from app.common.constants import API_V1
from app.configs.database import get_db
from app.configs.redis import RedisClient, get_redis
from app.configs.base_status import BaseStatus

router = APIRouter(prefix=API_V1 + "/example", tags=["Example"])


@router.get(
    "/hello",
    response_model=BaseResponse[dict],
    summary="Hello World",
    description="간단한 Hello World API입니다.",
)
async def hello_world() -> BaseResponse[dict]:
    """
    Hello World API

    Returns:
        BaseResponse: 인사 메시지
    """
    return success_response(
        message="Hello, DevDeb!",
        data={"greeting": "안녕하세요, FastAPI입니다!"},
    )


@router.get(
    "/redis/test",
    response_model=BaseResponse[dict],
    summary="Redis 테스트",
    description="Redis 연결 및 기본 작업을 테스트합니다.",
)
async def test_redis(
    redis: RedisClient = Depends(get_redis),
) -> BaseResponse[dict]:
    """
    Redis 테스트 API

    Redis에 데이터를 저장하고 조회합니다.

    Args:
        redis: Redis 클라이언트

    Returns:
        BaseResponse: Redis 작업 결과
    """
    # 데이터 저장
    await redis.set("test_key", "test_value", ex=60)

    # 데이터 조회
    value = await redis.get("test_key")

    # JSON 저장/조회 테스트
    test_data = {"name": "홍길동", "age": 30}
    await redis.set_json("test_json", test_data, ex=60)
    json_value = await redis.get_json("test_json")

    return success_response(
        message="Redis 테스트 성공",
        data={
            "string_value": value,
            "json_value": json_value,
        },
    )


@router.get(
    "/database/test",
    response_model=BaseResponse[dict],
    summary="데이터베이스 테스트",
    description="데이터베이스 연결을 테스트합니다.",
)
async def test_database(
    db: AsyncSession = Depends(get_db),
) -> BaseResponse[dict]:
    """
    데이터베이스 테스트 API

    데이터베이스 연결을 확인합니다.

    Args:
        db: 데이터베이스 세션

    Returns:
        BaseResponse: 데이터베이스 연결 상태
    """
    from sqlalchemy import text

    # 간단한 쿼리 실행
    result = await db.execute(text("SELECT 1 as test"))
    row = result.first()

    return success_response(
        message="데이터베이스 연결 성공",
        data={
            "connected": True,
            "test_query_result": row[0] if row else None,
        },
    )


@router.get(
    "/status/test",
    response_model=BaseResponse[dict],
    summary="다양한 상태 코드 테스트",
    description="다양한 BaseStatus를 테스트합니다.",
)
async def test_status() -> BaseResponse[dict]:
    """
    상태 코드 테스트 API

    CREATED 상태로 응답합니다.

    Returns:
        BaseResponse: 생성 성공 응답
    """
    return success_response(
        message="리소스가 생성되었습니다",
        data={"id": 1, "name": "새로운 리소스"},
        status=BaseStatus.CREATED,
    )
