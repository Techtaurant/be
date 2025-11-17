"""
API 예외 클래스 정의

새로운 예외 타입 추가 시 BaseAPIException을 확장하여 사용
"""

from app.configs.base_status import BaseStatus


class BaseAPIException(Exception):
    """
    모든 API 예외의 기본 클래스

    새로운 예외를 추가할 때는 이 클래스를 상속받아 구현합니다.
    """

    def __init__(
        self,
        status: BaseStatus,
        message: str,
    ):
        """
        Args:
            status: CustomStatus 열거형
            message: 에러 메시지
            http_status_code: HTTP 상태 코드 (지정하지 않으면 status에서 자동 매핑)
        """
        self.status = status
        self.message = message
        self.http_status_code = status.http_code
        super().__init__(self.message)


# 4xx 클라이언트 에러
class BadRequestException(BaseAPIException):
    """잘못된 요청"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.BAD_REQUEST,
            message or BaseStatus.BAD_REQUEST.message,
        )


class UnauthorizedException(BaseAPIException):
    """인증 필요"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.UNAUTHORIZED,
            message or BaseStatus.UNAUTHORIZED.message,
        )


class ForbiddenException(BaseAPIException):
    """권한 없음"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.FORBIDDEN,
            message or BaseStatus.FORBIDDEN.message,
        )


class NotFoundException(BaseAPIException):
    """리소스를 찾을 수 없음"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.NOT_FOUND,
            message or BaseStatus.NOT_FOUND.message,
        )


class ConflictException(BaseAPIException):
    """리소스 충돌"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.CONFLICT,
            message or BaseStatus.CONFLICT.message,
        )


class ValidationException(BaseAPIException):
    """유효성 검증 실패"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.VALIDATION_ERROR,
            message or BaseStatus.VALIDATION_ERROR.message,
        )


# 5xx 서버 에러
class InternalServerException(BaseAPIException):
    """내부 서버 오류"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.INTERNAL_SERVER_ERROR,
            message or BaseStatus.INTERNAL_SERVER_ERROR.message,
        )


class DatabaseException(BaseAPIException):
    """데이터베이스 오류"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.DATABASE_ERROR,
            message or BaseStatus.DATABASE_ERROR.message,
        )


class RedisException(BaseAPIException):
    """Redis 오류"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.REDIS_ERROR,
            message or BaseStatus.REDIS_ERROR.message,
        )
