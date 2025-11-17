"""
공통 응답 스키마

모든 API 응답은 일관된 형식을 가집니다.
"""

from datetime import UTC, datetime
from typing import Any, Generic, TypeVar

from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field

from app.configs.base_status import BaseStatus

# Generic 타입 변수
T = TypeVar("T")


class BaseResponse(BaseModel, Generic[T]):
    """
    모든 API 응답의 기본 구조

    성공 응답과 실패 응답 모두 이 구조를 따릅니다.

    Attributes:
        success: 성공 여부
        status: BaseStatus 열거형
        message: 응답 메시지
        data: 응답 데이터 (선택적)
        timestamp: 응답 시간 (UTC)

    Example:
        성공 응답:
        ```json
        {
            "success": true,
            "status": "SUCCESS",
            "message": "요청이 성공했습니다",
            "data": { "id": 1, "name": "홍길동" },
            "timestamp": "2024-01-01T00:00:00"
        }
        ```

        실패 응답:
        ```json
        {
            "success": false,
            "status": "NOT_FOUND",
            "message": "사용자를 찾을 수 없습니다",
            "data": null,
            "timestamp": "2024-01-01T00:00:00"
        }
        ```
    """

    success: bool = Field(..., description="성공 여부")
    status: str = Field(..., description="상태 코드")
    message: str = Field(..., description="응답 메시지")
    data: T | None = Field(None, description="응답 데이터")
    timestamp: datetime = Field(
        default_factory=lambda: datetime.now(UTC),
        description="응답 시간 (UTC)",
    )

    model_config = ConfigDict(
        json_schema_extra={
            "example": {
                "success": True,
                "status": "SUCCESS",
                "message": "요청이 성공했습니다",
                "data": None,
                "timestamp": "2024-01-01T00:00:00",
            }
        }
    )


def success_response(
    message: str = "요청이 성공했습니다",
    data: Any = None,
    status: BaseStatus = BaseStatus.SUCCESS,
) -> BaseResponse:
    """
    성공 응답 생성 헬퍼 함수

    Args:
        message: 응답 메시지
        data: 응답 데이터
        status: BaseStatus (기본값: SUCCESS)

    Returns:
        BaseResponse: 성공 응답

    Example:
        ```python
        @router.get("/users/{user_id}")
        async def get_user(user_id: int):
            user = await get_user_by_id(user_id)
            return success_response(
                message="사용자 조회 성공",
                data=user
            )
        ```
    """
    return BaseResponse(
        success=True,
        status=status.custom_code,
        message=message,
        data=data,
    )


def error_json_response(
    status: BaseStatus,
    message: str | None = None,
    data: Any = None,
) -> JSONResponse:
    """
    에러 응답을 JSONResponse로 생성하는 헬퍼 함수

    Exception Handler에서 사용됩니다.

    Args:
        status: Status Enum (BaseStatus 또는 도메인별 Status)
               - status_code 속성 필요
               - http_status_code 속성 필수
               - message 속성 권장
        message: 에러 메시지 (선택적, None이면 status.message 사용)
        data: 추가 데이터 (선택적)

    Returns:
        JSONResponse: HTTP 상태 코드가 설정된 JSON 응답

    Example:
        ```python
        # Exception Handler에서 사용
        @app.exception_handler(BaseAPIException)
        async def base_api_exception_handler(request: Request, exc: BaseAPIException):
            return error_json_response(status=exc.status, message=exc.message)
        ```
    """

    if message is None:
        message = status.message

    return JSONResponse(
        status_code=status.http_code,
        content=BaseResponse(
            success=False,
            status=status.custom_code,
            message=message,
            data=data,
        ).model_dump(mode="json"),
    )
