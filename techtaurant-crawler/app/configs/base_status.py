"""
API 응답 상태 코드 관리

코드 번호 체계:
- 1000번대: 공통 상태 코드 (BaseStatus)
  - 1001-1099: 2xx 성공
  - 1100-1199: 4xx 클라이언트 에러
  - 1200-1299: 5xx 서버 에러
- 2000번대 이상: 각 도메인별로 할당 (예: UserStatus, PostStatus 등)

주의: 새로운 상태 코드 추가 시 README.md에 기록하여 중복을 방지해야 합니다.
"""

from enum import Enum


class BaseStatus(Enum):
    """
    API 응답 상태 코드

    각 상태는 다음 정보를 포함합니다:
    - custom_code: 도메인별 숫자 코드 (예: "1001")
    - http_code: HTTP status code (예: 200)
    - message: 기본 메시지 (예: "요청이 성공했습니다")

    Example:
        ```python
        status = BaseStatus.SUCCESS
        print(status.custom_code)    # "1001"
        print(status.http_code)      # 200
        print(status.message)        # "요청이 성공했습니다"
        ```
    """

    # 1001-1099: 2xx 성공
    SUCCESS = ("1001", 200, "요청이 성공했습니다")
    CREATED = ("1002", 201, "리소스가 생성되었습니다")

    # 1100-1199: 4xx 클라이언트 에러
    BAD_REQUEST = ("1100", 400, "잘못된 요청입니다")
    UNAUTHORIZED = ("1101", 401, "인증이 필요합니다")
    FORBIDDEN = ("1102", 403, "접근 권한이 없습니다")
    NOT_FOUND = ("1103", 404, "리소스를 찾을 수 없습니다")
    CONFLICT = ("1104", 409, "리소스 충돌이 발생했습니다")
    VALIDATION_ERROR = ("1105", 422, "유효성 검증에 실패했습니다")

    # 1200-1299: 5xx 서버 에러
    INTERNAL_SERVER_ERROR = ("1200", 500, "내부 서버 오류가 발생했습니다")
    DATABASE_ERROR = ("1201", 500, "데이터베이스 오류가 발생했습니다")
    REDIS_ERROR = ("1202", 500, "Redis 오류가 발생했습니다")
    UNKNOWN_ERROR = ("1203", 500, "알 수 없는 오류가 발생했습니다")

    def __init__(self, custom_code: str, http_code: int, message: str):
        """
        BaseStatus 초기화

        Args:
            custom_code: 도메인별 숫자 코드 (예: "1001")
            http_code: HTTP 상태 코드
            message: 기본 메시지
        """
        self.custom_code = custom_code
        self.http_code = http_code
        self.message = message
