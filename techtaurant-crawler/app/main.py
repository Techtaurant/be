"""
FastAPI 애플리케이션 진입점

애플리케이션 인스턴스를 생성하고 실행합니다.
"""

import uvicorn

from app.configs.app import create_app


def main():
    uvicorn.run(app="app.main:app", host="0.0.0.0", port=8000, reload=True)


if __name__ == "__main__":
    main()


# FastAPI 애플리케이션 인스턴스 생성
app = create_app()
