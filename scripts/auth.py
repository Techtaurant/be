"""dev 환경 로그인 모듈.

SPRING_PROFILES_ACTIVE=dev 서버에서만 동작하는 테스트 사용자 로그인을 처리한다.
"""

import requests


def login(base_url: str, identifier: str = "test-user") -> str:
    """dev 로그인 API로 accessToken을 발급받아 반환한다.

    Args:
        base_url: 서버 기본 URL (예: http://localhost:8080)
        identifier: 테스트 사용자 식별자 (없으면 자동 생성됨)

    Returns:
        accessToken 문자열

    Raises:
        RuntimeError: 로그인 실패 또는 서버 응답 이상 시
    """
    resp = requests.post(
        f"{base_url}/open-api/dev/auth/login",
        json={"identifier": identifier, "password": "dev-password"},
    )

    if not resp.ok:
        raise RuntimeError(
            f"로그인 실패 (HTTP {resp.status_code}). "
            "서버가 dev 프로파일로 실행 중인지 확인하세요.\n"
            f"응답: {resp.text}"
        )

    token = resp.cookies.get("accessToken")
    if not token:
        raise RuntimeError("응답 쿠키에 accessToken이 없습니다.")

    return token
