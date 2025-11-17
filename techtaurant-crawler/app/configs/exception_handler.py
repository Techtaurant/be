"""
전역 예외 핸들러 등록

FastAPI 애플리케이션의 전역 예외 핸들러를 등록합니다.
"""

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.common.base_response import error_json_response
from app.configs.base_status import BaseStatus
from app.configs.config import settings
from app.configs.exceptions import BaseAPIException


def register_exception_handlers(app: FastAPI) -> None:
    """
    전역 예외 핸들러 등록

    Args:
        app: FastAPI 인스턴스
    """

    @app.exception_handler(BaseAPIException)
    async def base_exception_handler(
        request: Request,
        exc: BaseAPIException,
    ) -> JSONResponse:
        """
        BaseAPIException 처리

        모든 커스텀 예외를 처리하여 일관된 응답 형식을 반환합니다.

        Args:
            request: FastAPI Request
            exc: BaseAPIException

        Returns:
            JSONResponse: 에러 응답
        """
        return error_json_response(status=exc.status, message=exc.message)

    @app.exception_handler(Exception)
    async def general_exception_handler(
        request: Request,
        exc: Exception,
    ) -> JSONResponse:
        """
        일반 예외 처리

        정의되지 않은 예외를 UNKNOWN_ERROR 상태 코드로 변환하여 처리합니다.

        Args:
            request: FastAPI Request
            exc: Exception

        Returns:
            JSONResponse: 에러 응답

        Example Response:
            {
                "success": false,
                "status": "UNKNOWN_ERROR",
                "message": "알 수 없는 오류가 발생했습니다",
                "data": null,
                "timestamp": "2024-01-01T00:00:00"
            }
        """
        # DEBUG 모드에서는 상세 에러 메시지 표시
        message = str(exc) if settings.DEBUG else None

        return error_json_response(status=BaseStatus.UNKNOWN_ERROR, message=message)
