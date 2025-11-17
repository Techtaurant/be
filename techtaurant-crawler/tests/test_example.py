"""
Example API 엔드포인트 테스트
"""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_hello_endpoint() -> None:
    """
    Hello 엔드포인트가 정상적으로 응답하는지 테스트합니다.
    """
    response = client.get("/api/v1/example/hello")

    assert response.status_code == 200

    data = response.json()
    assert data["success"] is True
    assert data["status"] == "SUCCESS"
    assert "greeting" in data["data"]


def test_exception_endpoint() -> None:
    """
    Exception 테스트 엔드포인트가 404를 반환하는지 테스트합니다.
    """
    response = client.get("/api/v1/example/exception/test")

    assert response.status_code == 404

    data = response.json()
    assert data["success"] is False
    assert data["status"] == "NOT_FOUND"


def test_status_endpoint() -> None:
    """
    Status 테스트 엔드포인트가 CREATED 상태를 반환하는지 테스트합니다.
    """
    response = client.get("/api/v1/example/status/test")

    assert response.status_code == 200

    data = response.json()
    assert data["success"] is True
    assert data["status"] == "CREATED"
    assert data["data"]["id"] == 1
