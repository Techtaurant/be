## 프로젝트 구조

### Hexagonal Architecture (Ports & Adapters)

이 프로젝트는 **Hexagonal Architecture** 패턴을 따릅니다. 비즈니스 로직을 인프라로부터 독립시켜 테스트 용이성과 유지보수성을 높입니다.

```
├── app/
│   ├── main.py                 # FastAPI 애플리케이션 진입점
│   ├── core/                   # 핵심 공통 모듈
│   │   ├── config.py          # 환경 변수 관리
│   │   ├── database.py        # 데이터베이스 연결
│   │   ├── redis.py           # Redis 클라이언트
│   │   ├── status.py          # 공통 CustomStatus Enum
│   │   └── exceptions.py      # 공통 예외 클래스 (CustomApiException)
│   ├── schemas/               # 공통 Pydantic 스키마
│   │   └── common.py         # 공통 Response
│   ├── domains/               # 도메인별 모듈 (Hexagonal Architecture)
│   │   ├── order/            # 주문 도메인 (예시)
│   │   │   ├── application/  # 애플리케이션 레이어 (유스케이스 조율)
│   │   │   │   ├── dto/
│   │   │   │   │   ├── commands.py    # CreateOrderCommand, PlaceOrderCommand
│   │   │   │   │   └── results.py     # CreateOrderResult, OrderDto
│   │   │   │   ├── service.py         # OrderService (비즈니스 로직)
│   │   │   │   └── coordinator.py     # OrderCoordinator (여러 서비스 조율)
│   │   │   ├── domain/       # 도메인 레이어 (비즈니스 규칙)
│   │   │   │   ├── entities.py        # Order, OrderItem (도메인 엔티티)
│   │   │   │   └── status.py          # OrderStatus (도메인 전용 Status)
│   │   │   └── infrastructure/  # 인프라 레이어 (기술 세부사항)
│   │   │       ├── inbound/    # Inbound Adapter (외부 → 애플리케이션)
│   │   │       │   ├── dto/
│   │   │       │   │   ├── requests.py   # CreateOrderRequest, PlaceOrderRequest
│   │   │       │   │   └── responses.py  # CreateOrderResponse
│   │   │       │   └── api.py            # OrderController (FastAPI Router)
│   │   │       └── outbound/   # Outbound Adapter (애플리케이션 → 외부)
│   │   │           ├── repositories.py   # OrderRepository, OrderItemRepository
│   │   │           └── clients/
│   │   │               ├── point_client.py    # PointApiClient
│   │   │               └── product_client.py  # ProductApiClient
│   │   └── user/             # 사용자 도메인 (예시)
│   │       ├── application/
│   │       ├── domain/
│   │       └── infrastructure/
│   └── api/                   # API 라우터 통합
│       └── v1/
│           └── router.py     # 도메인별 라우터 등록
├── alembic/                   # 데이터베이스 마이그레이션
├── tests/                     # 테스트
│   └── domains/              # 도메인별 테스트
│       ├── order/
│       └── user/
├── docker-compose.yml         # Docker Compose (PostgreSQL, Redis)
├── pyproject.toml            # 프로젝트 설정
└── .env                      # 환경 변수
```

### 아키텍처 레이어 설명

#### 1. **Domain Layer** (도메인 레이어)
비즈니스 로직의 핵심으로, 프레임워크나 인프라에 의존하지 않습니다.

- **entities.py**: 도메인 엔티티 (Order, OrderItem 등)
  - 비즈니스 규칙과 불변성 유지
  - SQLAlchemy 모델이 아닌 순수 도메인 객체
- **status.py**: 도메인 전용 Status Enum
  - 도메인별 상태 코드 정의

**SOLID 원칙:**
- Single Responsibility: 각 엔티티는 하나의 비즈니스 개념만 표현
- Dependency Inversion: 인프라에 의존하지 않음

#### 2. **Application Layer** (애플리케이션 레이어)
유스케이스를 구현하고 도메인 로직을 조율합니다.

- **service.py**: 비즈니스 로직 서비스
  - 단일 유스케이스 처리
  - 도메인 엔티티 조작
- **coordinator.py**: 여러 서비스 조율
  - 복잡한 유스케이스 처리 (트랜잭션, 보상 처리 등)
  - 여러 서비스 호출 조합
- **dto/**: Command, Result 객체
  - **commands.py**: 입력 명령 (CreateOrderCommand)
  - **results.py**: 출력 결과 (CreateOrderResult)

**SOLID 원칙:**
- Single Responsibility: 각 서비스는 하나의 유스케이스만 처리
- Open/Closed: 새로운 유스케이스 추가 시 기존 코드 수정 불필요

#### 3. **Infrastructure Layer** (인프라 레이어)
기술적 세부사항을 처리합니다.

##### 3.1. **Inbound Adapters** (인바운드 어댑터)
외부 요청을 애플리케이션으로 전달합니다.

- **api.py**: FastAPI 라우터 (Controller 역할)
  - HTTP 요청을 Application Layer로 전달
  - Request DTO를 Command로 변환
- **dto/requests.py**: HTTP Request DTO
- **dto/responses.py**: HTTP Response DTO

##### 3.2. **Outbound Adapters** (아웃바운드 어댑터)
애플리케이션이 필요로 하는 외부 리소스를 제공합니다.

- **repositories.py**: 데이터베이스 접근
  - SQLAlchemy를 사용한 영속성 처리
  - 도메인 엔티티를 DB 모델로 변환
- **clients/**: 외부 API 클라이언트
  - 다른 마이크로서비스 또는 외부 API 호출
  - 예: PointApiClient, ProductApiClient

**SOLID 원칙:**
- Dependency Inversion: Application Layer는 인터페이스에 의존, Infrastructure가 구현
- Open/Closed: 새로운 어댑터 추가 시 기존 코드 수정 불필요

### 아키텍처 장점

1. **비즈니스 로직의 독립성**
   - Domain Layer는 프레임워크, 데이터베이스, 외부 API에 독립적
   - 테스트 용이성 극대화

2. **유연한 변경**
   - 데이터베이스 변경 (PostgreSQL → MongoDB): Outbound Adapter만 수정
   - API 변경 (REST → GraphQL): Inbound Adapter만 수정
   - 비즈니스 로직은 변경 불필요

3. **명확한 책임 분리**
   - 각 레이어가 명확한 역할 수행
   - 코드 가독성 및 유지보수성 향상

4. **테스트 용이성**
   - Domain Layer: 순수 단위 테스트
   - Application Layer: Mock Adapter로 테스트
   - Infrastructure Layer: 통합 테스트

## 시작하기

### 1. 환경 설정

```bash
# .env 파일 생성
cp .env.example .env

# 환경 변수 수정 (필요시)
vi .env
```

### 2. Docker로 실행 (권장)

Docker Compose를 사용하면 PostgreSQL, Redis, FastAPI 애플리케이션을 한 번에 실행할 수 있습니다.

```bash
# 모든 서비스 실행 (PostgreSQL 17 + Redis 7 + FastAPI)
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 컨테이너 상태 확인
docker-compose ps

# 특정 서비스만 재시작
docker-compose restart app

# 모든 서비스 중지
docker-compose down

# 볼륨까지 삭제 (데이터베이스 데이터도 삭제됨)
docker-compose down -v
```

**포함된 서비스:**
- `db`: PostgreSQL 17-alpine (포트: 5432)
- `redis`: Redis 7-alpine (포트: 6379)
- `app`: FastAPI 애플리케이션 (포트: 8000)

**접속 정보:**
- API: http://localhost:8000
- Swagger UI: http://localhost:8000/swagger-ui/index.html
- ReDoc: http://localhost:8000/redoc
- Health Check: http://localhost:8000/health

### 3. 로컬에서 실행

Docker 없이 로컬 환경에서 실행하려면 PostgreSQL과 Redis를 별도로 설치해야 합니다.

#### 3.1. PostgreSQL, Redis 컨테이너만 실행

```bash
# PostgreSQL, Redis 컨테이너만 실행
docker-compose up -d db redis

# 컨테이너 상태 확인
docker-compose ps
```

#### 3.2. 의존성 설치

```bash
# uv를 사용하여 의존성 설치
uv sync

# 가상환경 활성화
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate   # Windows
```

#### 3.3. 데이터베이스 마이그레이션

```bash
# Docker 환경에서 마이그레이션 (권장)
docker-compose exec app alembic revision --autogenerate -m "Initial migration"
docker-compose exec app alembic upgrade head

# 로컬 환경에서 마이그레이션
alembic revision --autogenerate -m "Initial migration"
alembic upgrade head

# 롤백 (필요시)
alembic downgrade -1
```

#### 3.4. 서버 실행 (로컬)

```bash
# 개발 서버 실행 (Hot Reload)
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 프로덕션 서버 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

## Docker 유용한 명령어

### 개발 중 자주 사용하는 명령어

```bash
# 애플리케이션 로그 실시간 확인
docker-compose logs -f app

# 데이터베이스 로그 확인
docker-compose logs -f db

# 컨테이너 내부 접속
docker-compose exec app bash
docker-compose exec db psql -U devdeb -d devdeb

# Redis CLI 접속
docker-compose exec redis redis-cli

# 애플리케이션만 재빌드
docker-compose up -d --build app

# 모든 컨테이너 재빌드
docker-compose up -d --build
```

### 데이터베이스 관리

```bash
# PostgreSQL 데이터베이스 백업
docker-compose exec db pg_dump -U devdeb devdeb > backup.sql

# PostgreSQL 데이터베이스 복원
docker-compose exec -T db psql -U devdeb devdeb < backup.sql

# 데이터베이스 초기화 (주의: 모든 데이터 삭제)
docker-compose down -v
docker-compose up -d
```

### 문제 해결

```bash
# 모든 컨테이너 재시작
docker-compose restart

# 특정 컨테이너 재시작
docker-compose restart app

# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 리소스 사용량 확인
docker stats

# 네트워크 확인
docker network ls
docker network inspect devdeb-be-feat-init-fastapi_devdeb-network
```

## 코드 포맷팅

```bash
# Ruff 포맷팅 실행
ruff format .

# Ruff 린트 실행
ruff check .

# Ruff 린트 자동 수정
ruff check --fix .
```

## Pre-commit Hooks

프로젝트는 pre-commit hooks를 사용하여 자동으로 코드 품질을 검사합니다.

### 설치

```bash
# Pre-commit hooks 설치
pre-commit install
```

### Hook 종류 및 실행 시점

각 hook은 구분하기 쉽도록 별도로 설정되어 있습니다:

#### 1. 🎨 Ruff Format (코드 포맷팅)
- **실행 시점**: `git commit` 시
- **기능**: 코드 스타일 자동 수정
- **설정 위치**: `.pre-commit-config.yaml` (Ruff 섹션)

#### 2. 🔍 Ruff Lint (린팅)
- **실행 시점**: `git commit` 시
- **기능**: 코드 품질 검사 및 자동 수정
- **설정 위치**: `.pre-commit-config.yaml` (Ruff 섹션)

#### 3. 🔎 MyPy (타입 체크)
- **실행 시점**: `git push` 시
- **기능**: Python 타입 힌트 검증
- **설정 위치**: `.pre-commit-config.yaml` (MyPy 섹션) + `pyproject.toml` ([tool.mypy])
- **중요**: 타입 오류가 있으면 push가 차단됩니다!

#### 4. 🧪 Pytest (단위 테스트)
- **실행 시점**: `git push` 시
- **기능**: 모든 단위 테스트 실행
- **설정 위치**: `.pre-commit-config.yaml` (Pytest 섹션)
- **중요**: 테스트 실패 시 push가 차단됩니다!

### 수동 실행

```bash
# 모든 파일에 대해 모든 hook 실행
pre-commit run --all-files

# 특정 hook만 실행
pre-commit run ruff-format --all-files
pre-commit run ruff --all-files
pre-commit run mypy --all-files
pre-commit run pytest --all-files

# Staged 파일에만 실행
pre-commit run
```

### Hook 우회 (권장하지 않음)

긴급한 경우에만 사용하세요:

```bash
# commit hook 우회
git commit --no-verify -m "message"

# push hook 우회
git push --no-verify
```

## 테스트

```bash
# 전체 테스트 실행
pytest

# 커버리지와 함께 테스트
pytest --cov=app tests/
```

## 공통 Response 구조

모든 API는 다음과 같은 일관된 응답 형식을 반환합니다:

```json
{
  "success": true,
  "status": "SUCCESS",
  "message": "요청이 성공했습니다",
  "data": { ... },
  "timestamp": "2024-01-01T00:00:00"
}
```

### CustomStatus 코드

| Status | HTTP Code | 설명 |
|--------|-----------|------|
| SUCCESS | 200 | 요청 성공 |
| CREATED | 201 | 리소스 생성 성공 |
| BAD_REQUEST | 400 | 잘못된 요청 |
| UNAUTHORIZED | 401 | 인증 필요 |
| FORBIDDEN | 403 | 권한 없음 |
| NOT_FOUND | 404 | 리소스를 찾을 수 없음 |
| CONFLICT | 409 | 리소스 충돌 |
| VALIDATION_ERROR | 422 | 유효성 검증 실패 |
| INTERNAL_SERVER_ERROR | 500 | 내부 서버 오류 |
| DATABASE_ERROR | 500 | 데이터베이스 오류 |
| REDIS_ERROR | 500 | Redis 오류 |
| UNKNOWN_ERROR | 500 | 알 수 없는 오류 |

### CustomStatus 사용법

CustomStatus는 각 상태에 대해 세 가지 정보를 제공합니다:

1. **status_code**: Custom status code (예: "SUCCESS")
2. **http_status_code**: HTTP status code (예: 200)
3. **default_message**: 기본 메시지 (예: "요청이 성공했습니다")

**중요:** 모든 Exception은 **반드시 CustomApiException을 통해서만 발생**시켜야 합니다.

**참고:** 도메인별 전용 Status가 필요한 경우, [도메인별 Status 클래스 작성](#도메인별-status-클래스-작성) 섹션을 참고하세요.

#### 사용 예시

```python
from app.configs.status import CustomStatus
from app.configs.exceptions import CustomApiException

# 1. Status code 접근
status = CustomStatus.SUCCESS
print(status.status_code)  # "SUCCESS"

# 2. HTTP status code 접근
print(status.http_status_code)  # 200

# 3. 기본 메시지 접근
print(status.default_message)  # "요청이 성공했습니다"

# 4. 헬퍼 함수 사용
from app.configs.status import get_http_status_code, get_default_message

http_code = get_http_status_code(CustomStatus.NOT_FOUND)  # 404
message = get_default_message(CustomStatus.NOT_FOUND)  # "리소스를 찾을 수 없습니다"

# 5. CustomApiException 사용 (권장)
# 기본 메시지 사용
raise CustomApiException(status=CustomStatus.NOT_FOUND)

# 커스텀 메시지 사용
raise CustomApiException(
    status=CustomStatus.NOT_FOUND,
    message="사용자를 찾을 수 없습니다"
)

# 6. 정의되지 않은 Exception 발생 시
# Exception Handler가 자동으로 UNKNOWN_ERROR로 변환하여 응답
raise ValueError("예상치 못한 에러")  # → UNKNOWN_ERROR로 자동 변환
```

### 도메인별 Status 클래스 작성

각 도메인은 `CustomStatus`를 기반으로 도메인 전용 Status를 정의해야 합니다.

#### 1. 도메인 Status 정의 방법

각 도메인의 `status.py` 파일에 Enum 클래스를 정의합니다:

```python
# app/domains/user/status.py
from enum import Enum

class UserStatus(str, Enum):
    """사용자 도메인 전용 Status

    각 Status는 (status_code, http_status_code, default_message) 튜플로 정의됩니다.
    """
    USER_NOT_FOUND = ("USER_NOT_FOUND", 404, "사용자를 찾을 수 없습니다")
    USER_ALREADY_EXISTS = ("USER_ALREADY_EXISTS", 409, "이미 존재하는 사용자입니다")
    INVALID_PASSWORD = ("INVALID_PASSWORD", 400, "잘못된 비밀번호입니다")
    EMAIL_ALREADY_IN_USE = ("EMAIL_ALREADY_IN_USE", 409, "이미 사용 중인 이메일입니다")

    def __init__(self, status_code: str, http_status_code: int, default_message: str):
        self.status_code = status_code
        self.http_status_code = http_status_code
        self.default_message = default_message
```

#### 2. 도메인 Status 사용 예시

**중요:** 모든 Exception은 CustomApiException을 사용해야 합니다.

```python
# app/domains/user/application/service.py
from app.domains.user.domain.status import UserStatus
from app.configs.exceptions import CustomApiException

async def get_user_by_id(user_id: int):
    user = await repository.find_by_id(user_id)
    if not user:
        # 도메인 Status의 default_message 사용
        raise CustomApiException(status=UserStatus.USER_NOT_FOUND)
    return user
```

```python
# app/domains/user/infrastructure/inbound/api.py
from fastapi import APIRouter
from app.domains.user.domain.status import UserStatus
from app.configs.exceptions import CustomApiException
from app.schemas.common import success_response

router = APIRouter()

@router.post("/users")
async def create_user(
    request: CreateUserRequest,
    service: UserService = Depends(get_user_service)
):
    if await service.user_exists(request.email):
        # 도메인 Status 활용하여 일관된 에러 응답
        raise CustomApiException(status=UserStatus.USER_ALREADY_EXISTS)

    user = await service.create_user(request)
    return success_response(data=user, message="사용자가 생성되었습니다")
```

**자동 에러 응답:**
```json
{
  "success": false,
  "status": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다",
  "data": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

#### 3. 도메인 Status 코드 관리 규칙

**중요:** 도메인별로 Status 코드를 정의할 때 다음 규칙을 준수하세요:

1. **Status 코드 네이밍**: `{DOMAIN}_{ACTION/STATE}` 형식 사용
   - 예: `USER_NOT_FOUND`, `POST_DELETED`, `COMMENT_FORBIDDEN`

2. **중복 방지**: 다른 도메인과 Status 코드가 겹치지 않도록 도메인 접두사 사용

3. **README 기록**: 새로운 도메인 Status 추가 시 아래 표에 기록

4. **HTTP 상태 코드 매핑**: RESTful API 원칙에 따라 적절한 HTTP 상태 코드 사용

#### 4. 도메인별 Status 코드 목록

**User 도메인 (`app/domains/user/status.py`)**

| Status Code | HTTP Code | 설명 |
|------------|-----------|------|
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없습니다 |
| USER_ALREADY_EXISTS | 409 | 이미 존재하는 사용자입니다 |
| INVALID_PASSWORD | 400 | 잘못된 비밀번호입니다 |
| EMAIL_ALREADY_IN_USE | 409 | 이미 사용 중인 이메일입니다 |

**Post 도메인 (`app/domains/post/status.py`)** _(예시)_

| Status Code | HTTP Code | 설명 |
|------------|-----------|------|
| POST_NOT_FOUND | 404 | 게시물을 찾을 수 없습니다 |
| POST_ALREADY_DELETED | 410 | 이미 삭제된 게시물입니다 |
| POST_FORBIDDEN | 403 | 게시물에 접근할 권한이 없습니다 |

_새로운 도메인 추가 시 위 표에 Status 코드를 추가하세요._

## 환경 변수

`.env.example` 파일을 복사하여 `.env` 파일을 생성하고 필요한 값을 설정하세요.

### Docker Compose 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| POSTGRES_USER | PostgreSQL 사용자명 | devdeb |
| POSTGRES_PASSWORD | PostgreSQL 비밀번호 | devdeb123 |
| POSTGRES_DB | PostgreSQL 데이터베이스명 | devdeb |
| POSTGRES_PORT | PostgreSQL 포트 | 5432 |
| REDIS_PORT | Redis 포트 | 6379 |
| REDIS_PASSWORD | Redis 비밀번호 (선택) | (없음) |
| REDIS_DB | Redis 데이터베이스 번호 | 0 |

### 애플리케이션 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| APP_NAME | 애플리케이션 이름 | DevDeb API |
| APP_VERSION | 애플리케이션 버전 | 1.0.0 |
| DEBUG | 디버그 모드 | true |
| DATABASE_URL | PostgreSQL 연결 URL | postgresql+asyncpg://devdeb:devdeb123@localhost:5432/devdeb |
| DB_POOL_SIZE | DB 커넥션 풀 크기 | 5 |
| DB_MAX_OVERFLOW | DB 최대 오버플로우 | 10 |
| REDIS_HOST | Redis 호스트 | localhost (로컬) / redis (Docker) |
| REDIS_PORT | Redis 포트 | 6379 |
| CORS_ORIGINS | CORS 허용 Origin | ["http://localhost:3000","http://localhost:8000"] |

**주의:** Docker Compose 사용 시 `REDIS_HOST`는 자동으로 `redis`로 설정되며, `DATABASE_URL`은 자동으로 생성됩니다.

## 개발 가이드

### 새로운 도메인 추가 (Hexagonal Architecture)

Hexagonal Architecture를 따라 새로운 도메인을 추가할 때는 다음 단계를 따르세요:

#### 1. 도메인 디렉토리 구조 생성

```bash
# 도메인 루트 디렉토리
mkdir -p app/domains/{domain_name}

# Domain Layer
mkdir -p app/domains/{domain_name}/domain

# Application Layer
mkdir -p app/domains/{domain_name}/application/dto

# Infrastructure Layer
mkdir -p app/domains/{domain_name}/infrastructure/inbound/dto
mkdir -p app/domains/{domain_name}/infrastructure/outbound/clients
```

예: `mkdir -p app/domains/product`

#### 2. 도메인 파일 생성

각 레이어별로 필요한 파일을 생성합니다:

```bash
# Domain Layer
touch app/domains/product/domain/entities.py    # 도메인 엔티티
touch app/domains/product/domain/status.py      # 도메인 Status

# Application Layer
touch app/domains/product/application/service.py          # 비즈니스 로직 서비스
touch app/domains/product/application/coordinator.py      # 서비스 조율 (필요시)
touch app/domains/product/application/dto/commands.py     # Command 객체
touch app/domains/product/application/dto/results.py      # Result 객체

# Infrastructure/Inbound
touch app/domains/product/infrastructure/inbound/api.py             # FastAPI Router
touch app/domains/product/infrastructure/inbound/dto/requests.py    # HTTP Request DTO
touch app/domains/product/infrastructure/inbound/dto/responses.py   # HTTP Response DTO

# Infrastructure/Outbound
touch app/domains/product/infrastructure/outbound/repositories.py   # Repository
```

#### 3. 각 레이어별 파일 구현

##### 3.1. Domain Layer 구현

**`domain/entities.py` - 도메인 엔티티 정의**

```python
# app/domains/product/domain/entities.py
from dataclasses import dataclass
from datetime import datetime

@dataclass
class Product:
    """
    상품 도메인 엔티티

    [SOLID: Single Responsibility]
    비즈니스 규칙과 불변성만 관리합니다.
    데이터베이스나 프레임워크에 의존하지 않습니다.
    """
    id: int | None
    name: str
    price: int
    stock: int
    created_at: datetime | None = None
    updated_at: datetime | None = None

    def is_available(self) -> bool:
        """상품이 구매 가능한지 확인"""
        return self.stock > 0 and self.price > 0

    def decrease_stock(self, quantity: int) -> None:
        """재고 감소"""
        if quantity > self.stock:
            from app.domains.product.domain.status import ProductStatus
            from app.configs.exceptions import CustomApiException
            raise CustomApiException(status=ProductStatus.PRODUCT_OUT_OF_STOCK)
        self.stock -= quantity
```

**`domain/status.py` - 도메인 전용 Status 정의**

```python
# app/domains/product/domain/status.py
from enum import Enum

class ProductStatus(str, Enum):
    """상품 도메인 전용 Status"""
    PRODUCT_NOT_FOUND = ("PRODUCT_NOT_FOUND", 404, "상품을 찾을 수 없습니다")
    PRODUCT_OUT_OF_STOCK = ("PRODUCT_OUT_OF_STOCK", 400, "상품 재고가 부족합니다")
    INVALID_PRICE = ("INVALID_PRICE", 400, "잘못된 가격입니다")

    def __init__(self, status_code: str, http_status_code: int, default_message: str):
        self.status_code = status_code
        self.http_status_code = http_status_code
        self.default_message = default_message
```

**참고:** Status 코드를 정의한 후 [도메인별 Status 코드 목록](#4-도메인별-status-코드-목록)에 추가하세요.

##### 3.2. Application Layer 구현

**`application/dto/commands.py` - Command 객체 정의**

```python
# app/domains/product/application/dto/commands.py
from pydantic import BaseModel, Field

class CreateProductCommand(BaseModel):
    """상품 생성 명령"""
    name: str = Field(..., description="상품명", min_length=1, max_length=100)
    price: int = Field(..., description="가격", ge=0)
    stock: int = Field(..., description="재고", ge=0)
```

**`application/dto/results.py` - Result 객체 정의**

```python
# app/domains/product/application/dto/results.py
from pydantic import BaseModel, Field
from datetime import datetime

class ProductDto(BaseModel):
    """상품 DTO"""
    id: int = Field(..., description="상품 ID")
    name: str = Field(..., description="상품명")
    price: int = Field(..., description="가격")
    stock: int = Field(..., description="재고")
    created_at: datetime = Field(..., description="생성일시")

class CreateProductResult(BaseModel):
    """상품 생성 결과"""
    product: ProductDto
```

**`application/service.py` - 비즈니스 로직 서비스**

```python
# app/domains/product/application/service.py
from app.domains.product.domain.entities import Product
from app.domains.product.domain.status import ProductStatus
from app.domains.product.application.dto.commands import CreateProductCommand
from app.domains.product.application.dto.results import CreateProductResult, ProductDto
from app.configs.exceptions import CustomApiException

class ProductService:
    """
    상품 도메인 서비스

    [SOLID: Single Responsibility]
    상품 관련 유스케이스만 처리합니다.
    """

    def __init__(self, repository):
        """
        [SOLID: Dependency Inversion]
        구체적인 Repository가 아닌 인터페이스에 의존합니다.
        """
        self.repository = repository

    async def create_product(self, command: CreateProductCommand) -> CreateProductResult:
        """상품 생성 유스케이스"""
        # 도메인 엔티티 생성
        product = Product(
            id=None,
            name=command.name,
            price=command.price,
            stock=command.stock
        )

        # Repository를 통해 영속화
        saved_product = await self.repository.save(product)

        # Result 반환
        return CreateProductResult(
            product=ProductDto(
                id=saved_product.id,
                name=saved_product.name,
                price=saved_product.price,
                stock=saved_product.stock,
                created_at=saved_product.created_at
            )
        )

    async def get_product(self, product_id: int) -> ProductDto:
        """상품 조회 유스케이스"""
        product = await self.repository.find_by_id(product_id)

        if not product:
            raise CustomApiException(status=ProductStatus.PRODUCT_NOT_FOUND)

        return ProductDto(
            id=product.id,
            name=product.name,
            price=product.price,
            stock=product.stock,
            created_at=product.created_at
        )
```

##### 3.3. Infrastructure/Inbound 구현

**`infrastructure/inbound/dto/requests.py` - HTTP Request DTO**

```python
# app/domains/product/infrastructure/inbound/dto/requests.py
from pydantic import BaseModel, Field

class CreateProductRequest(BaseModel):
    """상품 생성 HTTP 요청"""
    name: str = Field(..., description="상품명", min_length=1, max_length=100)
    price: int = Field(..., description="가격", ge=0)
    stock: int = Field(..., description="재고", ge=0)
```

**`infrastructure/inbound/dto/responses.py` - HTTP Response DTO**

```python
# app/domains/product/infrastructure/inbound/dto/responses.py
from pydantic import BaseModel, Field
from datetime import datetime

class ProductResponse(BaseModel):
    """상품 HTTP 응답"""
    id: int = Field(..., description="상품 ID")
    name: str = Field(..., description="상품명")
    price: int = Field(..., description="가격")
    stock: int = Field(..., description="재고")
    created_at: datetime = Field(..., description="생성일시")
```

**`infrastructure/inbound/api.py` - FastAPI Router (Controller)**

```python
# app/domains/product/infrastructure/inbound/api.py
from fastapi import APIRouter, Depends
from app.domains.product.infrastructure.inbound.dto.requests import CreateProductRequest
from app.domains.product.infrastructure.inbound.dto.responses import ProductResponse
from app.domains.product.application.service import ProductService
from app.domains.product.application.dto.commands import CreateProductCommand
from app.domains.product.infrastructure.outbound.repositories import ProductRepository
from app.schemas.common import CommonResponse, success_response
from app.configs.database import get_db
from sqlalchemy.ext.asyncio import AsyncSession

router = APIRouter(prefix="/products", tags=["Product"])

def get_product_service(db: AsyncSession = Depends(get_db)) -> ProductService:
    """
    ProductService 의존성 주입

    [SOLID: Dependency Inversion]
    Controller는 Service 인터페이스에만 의존합니다.
    """
    repository = ProductRepository(db)
    return ProductService(repository)

@router.post("", response_model=CommonResponse[ProductResponse], status_code=201)
async def create_product(
    request: CreateProductRequest,
    service: ProductService = Depends(get_product_service)
):
    """상품 생성 API"""
    # Request를 Command로 변환
    command = CreateProductCommand(
        name=request.name,
        price=request.price,
        stock=request.stock
    )

    # Service 호출
    result = await service.create_product(command)

    # Result를 Response로 변환
    response = ProductResponse(
        id=result.product.id,
        name=result.product.name,
        price=result.product.price,
        stock=result.product.stock,
        created_at=result.product.created_at
    )

    return success_response(data=response, message="상품이 생성되었습니다")

@router.get("/{product_id}", response_model=CommonResponse[ProductResponse])
async def get_product(
    product_id: int,
    service: ProductService = Depends(get_product_service)
):
    """상품 조회 API"""
    product_dto = await service.get_product(product_id)

    response = ProductResponse(
        id=product_dto.id,
        name=product_dto.name,
        price=product_dto.price,
        stock=product_dto.stock,
        created_at=product_dto.created_at
    )

    return success_response(data=response)
```

##### 3.4. Infrastructure/Outbound 구현

**`infrastructure/outbound/repositories.py` - Repository**

```python
# app/domains/product/infrastructure/outbound/repositories.py
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import Column, Integer, String, DateTime, func, select
from app.configs.database import Base
from app.domains.product.domain.entities import Product

class ProductModel(Base):
    """
    SQLAlchemy 모델

    [SOLID: Single Responsibility]
    데이터베이스 매핑만 담당합니다.
    """
    __tablename__ = "products"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    price = Column(Integer, nullable=False)
    stock = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

class ProductRepository:
    """
    상품 Repository

    [SOLID: Dependency Inversion]
    Application Layer는 이 Repository의 인터페이스에 의존합니다.
    """

    def __init__(self, db: AsyncSession):
        self.db = db

    async def save(self, product: Product) -> Product:
        """상품 저장"""
        db_product = ProductModel(
            name=product.name,
            price=product.price,
            stock=product.stock
        )
        self.db.add(db_product)
        await self.db.commit()
        await self.db.refresh(db_product)

        # DB 모델을 도메인 엔티티로 변환
        return Product(
            id=db_product.id,
            name=db_product.name,
            price=db_product.price,
            stock=db_product.stock,
            created_at=db_product.created_at,
            updated_at=db_product.updated_at
        )

    async def find_by_id(self, product_id: int) -> Product | None:
        """상품 ID로 조회"""
        result = await self.db.execute(
            select(ProductModel).filter(ProductModel.id == product_id)
        )
        db_product = result.scalar_one_or_none()

        if not db_product:
            return None

        return Product(
            id=db_product.id,
            name=db_product.name,
            price=db_product.price,
            stock=db_product.stock,
            created_at=db_product.created_at,
            updated_at=db_product.updated_at
        )
```

#### 4. 라우터 등록

`app/api/v1/router.py`에 도메인 라우터를 등록합니다:

```python
# app/api/v1/router.py
from fastapi import APIRouter
from app.domains.product.infrastructure.inbound.api import router as product_router
from app.domains.user.infrastructure.inbound.api import router as user_router

api_router = APIRouter()

# 도메인별 라우터 등록
api_router.include_router(product_router)
api_router.include_router(user_router)
```

#### 5. 데이터베이스 마이그레이션

```bash
# 마이그레이션 파일 생성
alembic revision --autogenerate -m "Add product domain"

# 마이그레이션 적용
alembic upgrade head
```

#### 6. 테스트 작성

```bash
# 도메인별 테스트 디렉토리 생성
mkdir -p tests/domains/product
touch tests/domains/product/test_domain_entities.py      # 도메인 엔티티 테스트
touch tests/domains/product/test_application_service.py  # 서비스 테스트
touch tests/domains/product/test_infrastructure_api.py   # API 통합 테스트
```

### 도메인 추가 체크리스트 (Hexagonal Architecture)

#### Domain Layer
- [ ] `domain/entities.py` 작성 (도메인 엔티티)
- [ ] `domain/status.py` 작성 및 README에 Status 코드 기록

#### Application Layer
- [ ] `application/dto/commands.py` 작성 (Command 객체)
- [ ] `application/dto/results.py` 작성 (Result 객체)
- [ ] `application/service.py` 작성 (비즈니스 로직 서비스)
- [ ] `application/coordinator.py` 작성 (필요시, 여러 서비스 조율)

#### Infrastructure/Inbound
- [ ] `infrastructure/inbound/dto/requests.py` 작성 (HTTP Request DTO)
- [ ] `infrastructure/inbound/dto/responses.py` 작성 (HTTP Response DTO)
- [ ] `infrastructure/inbound/api.py` 작성 (FastAPI Router)

#### Infrastructure/Outbound
- [ ] `infrastructure/outbound/repositories.py` 작성 (Repository, DB 모델)
- [ ] `infrastructure/outbound/clients/` 작성 (외부 API 클라이언트, 필요시)

#### 통합
- [ ] `app/api/v1/router.py`에 라우터 등록
- [ ] Alembic 마이그레이션 생성 및 적용
- [ ] 레이어별 테스트 코드 작성
- [ ] Swagger 문서 확인 (http://localhost:8000/docs)

### CustomApiException 사용법

**중요:** 이 프로젝트에서는 **모든 Exception을 CustomApiException을 통해서만 발생**시켜야 합니다.

#### 기본 사용법

CustomApiException은 도메인별 Status Enum을 받아서 자동으로 일관된 에러 응답을 생성합니다.

```python
from app.configs.exceptions import CustomApiException
from app.configs.status import CustomStatus
from app.domains.product.domain.status import ProductStatus

# 1. 기본 사용 (Status의 default_message 사용)
raise CustomApiException(status=ProductStatus.PRODUCT_NOT_FOUND)

# 2. 커스텀 메시지 사용
raise CustomApiException(
    status=ProductStatus.PRODUCT_NOT_FOUND,
    message="ID 123번 상품을 찾을 수 없습니다"
)

# 3. 공통 CustomStatus 사용
raise CustomApiException(status=CustomStatus.BAD_REQUEST)

# 4. 여러 Status 타입 호환
raise CustomApiException(status=CustomStatus.NOT_FOUND)  # 공통 Status
raise CustomApiException(status=UserStatus.USER_NOT_FOUND)  # 도메인 Status
```

#### 자동 에러 응답 형식

CustomApiException이 발생하면 FastAPI Exception Handler가 자동으로 다음 형식으로 응답합니다:

```json
{
  "success": false,
  "status": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다",
  "data": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

HTTP 상태 코드는 Status Enum의 `http_status_code`가 자동으로 사용됩니다.

#### 정의되지 않은 Exception 처리

정의되지 않은 Exception이 발생해도 자동으로 `UNKNOWN_ERROR`로 변환되어 일관된 응답 형식을 유지합니다.

```python
# 예상치 못한 에러 발생
raise ValueError("예상치 못한 에러")

# 자동으로 다음과 같이 응답됨:
{
  "success": false,
  "status": "UNKNOWN_ERROR",
  "message": "알 수 없는 오류가 발생했습니다",  # DEBUG=true일 때는 실제 에러 메시지
  "data": null,
  "timestamp": "2024-01-01T00:00:00"
}
```

#### 장점

1. **일관된 에러 응답**: 모든 도메인에서 동일한 형식의 에러 응답
2. **타입 안정성**: Status Enum을 통한 타입 체크
3. **유지보수 용이**: Status 코드 변경 시 한 곳만 수정
4. **자동 문서화**: Swagger에 Status 코드가 자동으로 문서화
5. **예외 안전성**: 정의되지 않은 Exception도 자동으로 처리
6. **디버깅 용이**: DEBUG 모드에서 상세 에러 메시지 제공

#### 사용 규칙

**필수:**
- ❌ `raise NotFoundException()` - 사용 금지
- ❌ `raise HTTPException()` - 사용 금지
- ✅ `raise CustomApiException(status=...)` - 항상 사용

**권장:**
- 도메인별 Status를 우선 사용 (예: `UserStatus.USER_NOT_FOUND`)
- 공통 Status는 도메인별 Status가 없을 때만 사용
- 커스텀 메시지는 필요할 때만 사용 (기본 메시지 활용)

#### Exception Handler 동작 방식

FastAPI에 등록된 전역 Exception Handler가 자동으로 모든 Exception을 처리합니다:

```python
# app/main.py

@app.exception_handler(CustomApiException)
async def custom_api_exception_handler(request, exc):
    """CustomApiException을 일관된 형식으로 응답"""
    return JSONResponse(
        status_code=exc.http_status_code,
        content={
            "success": False,
            "status": exc.status_code,
            "message": exc.message,
            "data": None,
            "timestamp": datetime.now().isoformat(),
        },
    )

@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    """정의되지 않은 Exception을 UNKNOWN_ERROR로 변환"""
    message = str(exc) if settings.DEBUG else CustomStatus.UNKNOWN_ERROR.default_message
    return JSONResponse(
        status_code=CustomStatus.UNKNOWN_ERROR.http_status_code,
        content={
            "success": False,
            "status": CustomStatus.UNKNOWN_ERROR.status_code,
            "message": message,
            "data": None,
            "timestamp": datetime.now().isoformat(),
        },
    )
```

**처리 순서:**
1. CustomApiException 발생 → CustomApiException Handler 처리
2. 기타 Exception 발생 → UNKNOWN_ERROR로 자동 변환

**DEBUG 모드:**
- `DEBUG=true`: 상세한 에러 메시지 표시 (개발 환경)
- `DEBUG=false`: 기본 메시지만 표시 (프로덕션 환경)

### 예외 처리

**중요:** 모든 Exception은 CustomApiException을 통해서만 발생시켜야 합니다.

```python
from app.configs.exceptions import CustomApiException
from app.configs.status import CustomStatus

@router.get("/users/{user_id}")
async def get_user(user_id: int):
    user = await get_user_by_id(user_id)
    if not user:
        # CustomApiException 사용
        raise CustomApiException(
            status=CustomStatus.NOT_FOUND,
            message="사용자를 찾을 수 없습니다"
        )
    return success_response(data=user)
```

**도메인별 Status 사용:**
```python
from app.configs.exceptions import CustomApiException
from app.domains.user.domain.status import UserStatus

@router.get("/users/{user_id}")
async def get_user(user_id: int):
    user = await service.get_user(user_id)
    if not user:
        # 도메인 Status 사용 (권장)
        raise CustomApiException(status=UserStatus.USER_NOT_FOUND)
    return success_response(data=user)
```

## 라이선스

MIT
