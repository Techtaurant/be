"""
Health Check 엔드포인트 테스트
"""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_check() -> None:
    """
    Health check 엔드포인트가 정상적으로 응답하는지 테스트합니다.
    """
    response = client.get("/health")

    assert response.status_code == 200

    data = response.json()
    assert data["success"] is True
    assert data["status"] == "SUCCESS"
    assert "app_name" in data["data"]
    assert "version" in data["data"]


def test_health_check_data() -> None:
    """
    Health check 응답 데이터가 올바른지 테스트합니다.
    """
    response = client.get("/health")
    data = response.json()

    assert data["data"]["app_name"] == "DevDeb API"
    assert data["data"]["version"] == "1.0.0"
