"""
Pytest 설정 및 공통 fixture
"""

import pytest


@pytest.fixture(scope="session")
def test_app():
    """
    테스트용 FastAPI 애플리케이션을 반환합니다.
    """
    from app.main import app

    return app
