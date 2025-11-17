"""
API 예외 클래스 정의

새로운 예외 타입 추가 시 BaseAPIException을 확장하여 사용
"""

from app.configs.base_status import BaseStatus, get_default_message


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
            message or get_default_message(BaseStatus.BAD_REQUEST),
        )


class UnauthorizedException(BaseAPIException):
    """인증 필요"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.UNAUTHORIZED,
            message or get_default_message(BaseStatus.UNAUTHORIZED),
        )


class ForbiddenException(BaseAPIException):
    """권한 없음"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.FORBIDDEN,
            message or get_default_message(BaseStatus.FORBIDDEN),
        )


class NotFoundException(BaseAPIException):
    """리소스를 찾을 수 없음"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.NOT_FOUND,
            message or get_default_message(BaseStatus.NOT_FOUND),
        )


class ConflictException(BaseAPIException):
    """리소스 충돌"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.CONFLICT,
            message or get_default_message(BaseStatus.CONFLICT),
        )


class ValidationException(BaseAPIException):
    """유효성 검증 실패"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.VALIDATION_ERROR,
            message or get_default_message(BaseStatus.VALIDATION_ERROR),
        )


# 5xx 서버 에러
class InternalServerException(BaseAPIException):
    """내부 서버 오류"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.INTERNAL_SERVER_ERROR,
            message or get_default_message(BaseStatus.INTERNAL_SERVER_ERROR),
        )


class DatabaseException(BaseAPIException):
    """데이터베이스 오류"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.DATABASE_ERROR,
            message or get_default_message(BaseStatus.DATABASE_ERROR),
        )


class RedisException(BaseAPIException):
    """Redis 오류"""

    def __init__(self, message: str | None = None):
        super().__init__(
            BaseStatus.REDIS_ERROR,
            message or get_default_message(BaseStatus.REDIS_ERROR),
        )
