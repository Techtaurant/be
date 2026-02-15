# Redis 의존성 제거 및 Caffeine 캐시 교체

## Business Goal
프로젝트 전체에서 Redis 의존성을 제거하여 인프라 복잡도를 낮추고, 캐싱 기능은 Caffeine 인메모리 캐시로 교체한다.
나중에 Redis로 다시 교체할 수 있도록 캐시 인터페이스 추상화를 도입한다.

## Scope
- **In Scope**: main-server Redis 제거 + Caffeine CacheManager 교체 + 캐시 인터페이스 추상화, techtaurant-crawler Redis 제거, infra docker-compose Redis 제거
- **Out of Scope**: `.claude/`, `.gemini/` 내 면접/문서 스킬 파일, techtaurant-crawler README.md

## Codebase Analysis Summary
- `TokenCacheManager`는 Spring `CacheManager` 추상화를 사용하므로 Redis에 직접 의존하지 않음
- `RedisTemplate` Bean은 `RedisConfig.kt` 내부에서만 정의되고 외부에서 사용하지 않음
- Caffeine은 이미 `build.gradle.kts`에 존재 (`caffeine:3.1.8`)
- techtaurant-crawler의 `RedisClient`는 비즈니스 코드에서 사용하지 않음

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `main-server/build.gradle.kts` | Gradle 의존성 | Modify (redis 제거) |
| `main-server/src/main/kotlin/.../security/config/RedisConfig.kt` | Redis 설정 | Delete |
| `main-server/src/main/kotlin/.../security/config/CacheType.kt` | 캐시 타입 Enum | Modify (TTL 추가) |
| `main-server/src/main/kotlin/.../security/cache/TokenCacheManager.kt` | 캐시 매니저 | Modify (인터페이스 추출) |
| `main-server/src/main/kotlin/.../security/cache/TokenCachePort.kt` | 캐시 인터페이스 | Create |
| `main-server/src/main/kotlin/.../security/config/CaffeineCacheConfig.kt` | Caffeine 캐시 설정 | Create |
| `main-server/src/main/resources/application.yml` | 앱 설정 | Modify (redis 섹션 제거) |
| `main-server/src/test/resources/application-test.yml` | 테스트 설정 | Modify (redis 섹션 제거) |
| `main-server/src/test/kotlin/.../base/IntegrationTest.kt` | 통합 테스트 베이스 | Modify (redis container 제거) |
| `main-server/.env.example` | 환경변수 예시 | Modify (redis 변수 제거) |
| `main-server/src/main/kotlin/.../common/lock/DistributedLock.kt` | 분산 락 인터페이스 | Modify (주석 수정) |
| `techtaurant-crawler/pyproject.toml` | Python 의존성 | Modify (redis 제거) |
| `techtaurant-crawler/app/configs/redis.py` | Redis 클라이언트 | Delete |
| `techtaurant-crawler/app/configs/app.py` | 앱 라이프사이클 | Modify (redis 초기화 제거) |
| `techtaurant-crawler/app/configs/exceptions.py` | 예외 정의 | Modify (RedisException 제거) |
| `techtaurant-crawler/app/configs/base_status.py` | 상태 코드 | Modify (REDIS_ERROR 제거) |
| `techtaurant-crawler/app/configs/config.py` | 설정 | Modify (redis 설정 제거) |
| `techtaurant-crawler/.pre-commit-config.yaml` | Pre-commit 설정 | Modify (types-redis 제거) |
| `techtaurant-crawler/.env.example` | 환경변수 예시 | Modify (redis 변수 제거) |
| `infra/docker-compose.yml` | Docker Compose | Modify (redis 서비스/볼륨 제거) |
| `infra/.env.example` | 환경변수 예시 | Modify (redis 변수 제거) |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 인터페이스 네이밍 | CODE_PRINCIPLES.md | Port 접미사 (TokenCachePort) |
| 주석 | CODE_PRINCIPLES.md | 한국어, 번호 없이 서술형 |
| JavaDoc | CODE_PRINCIPLES.md | @param, @returns 비즈니스 역할 명시 |
| KISS | CODE_PRINCIPLES.md | 불필요한 추상화 없이 단순 명확하게 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|-------------|
| 캐시 인터페이스 | TokenCachePort 인터페이스 추출 | 사용자 요청: Redis 교체 가능하도록 추상화 | Spring CacheManager만 사용 |
| CacheManager 구현체 | Caffeine | 이미 프로젝트에 존재, Spring Boot 공식 지원 | EhCache, Simple |
| TTL 관리 | CacheType enum에 TTL 포함 | 캐시 타입별 TTL 중앙 관리 | 별도 프로퍼티 |

## Implementation Todos

### Todo 1: main-server 캐시 인터페이스 추상화 및 Caffeine 교체
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Redis 의존성을 제거하고, TokenCachePort 인터페이스를 도입하여 Caffeine CacheManager로 교체한다.
- **Work**:
  - `build.gradle.kts`에서 `spring-boot-starter-data-redis` 의존성 제거
  - `RedisConfig.kt` 파일 삭제
  - `TokenCachePort.kt` 인터페이스 생성 (`security/cache/` 디렉토리):
    - `save(cacheType, key, value)`, `get(cacheType, key, type)`, `delete(cacheType, key)` 메서드
    - `saveRefreshToken(userId, token)`, `getRefreshToken(userId)`, `deleteRefreshToken(userId)` 메서드
  - `TokenCacheManager.kt` 수정: `TokenCachePort` 인터페이스 구현
  - `CacheType.kt` 수정: `ttlSeconds` 필드 추가 (REFRESH_TOKEN: 604800초 = 7일)
  - `CaffeineCacheConfig.kt` 생성 (`security/config/` 디렉토리):
    - `CacheManager` Bean: CacheType별 Caffeine 캐시 생성 (SimpleCacheManager 사용)
    - CacheType.entries에서 cacheName, TTL을 읽어 Caffeine 캐시 생성
  - `application.yml`에서 `spring.data.redis` 섹션 제거
  - `application-test.yml`에서 `spring.data.redis` 섹션 제거
  - `IntegrationTest.kt`에서 Redis Testcontainer(redisContainer)와 관련 DynamicPropertySource 제거
  - `main-server/.env.example`에서 Redis 환경변수 섹션 제거
  - `DistributedLock.kt` 주석에서 "추후 Redis 기반으로 확장 가능합니다" 문구 제거
  - `TokenCacheManager.kt` 주석에서 "Redis, EhCache 등" 문구를 "Caffeine 등" 으로 수정
  - 모든 `TokenCacheManager` 소비자(`OAuth2SuccessHandler`, `LogoutService`, `TokenRefreshService`, `TokenRefreshServiceTest`)의 타입을 `TokenCachePort`로 변경
- **Convention Notes**: 인터페이스명은 `TokenCachePort`, 구현체는 기존 `TokenCacheManager` 유지
- **Verification**: `cd main-server && ./gradlew build` 성공
- **Exit Criteria**: 빌드 성공, Redis 관련 import/코드 없음, 테스트 통과
- **Status**: completed

### Todo 2: techtaurant-crawler Redis 제거
- **Priority**: 1
- **Dependencies**: none
- **Goal**: techtaurant-crawler에서 사용하지 않는 Redis 관련 코드와 의존성을 제거한다.
- **Work**:
  - `techtaurant-crawler/app/configs/redis.py` 파일 삭제
  - `techtaurant-crawler/app/configs/app.py`에서 `redis_client` import 및 `connect()`/`disconnect()` 호출 제거
  - `techtaurant-crawler/app/configs/exceptions.py`에서 `RedisException` 클래스 제거
  - `techtaurant-crawler/app/configs/base_status.py`에서 `REDIS_ERROR` 항목 제거
  - `techtaurant-crawler/app/configs/config.py`에서 Redis 설정 섹션 제거
  - `techtaurant-crawler/pyproject.toml`에서 `redis[hiredis]`와 `types-redis` 의존성 제거
  - `techtaurant-crawler/.pre-commit-config.yaml`에서 `types-redis` 제거
  - `techtaurant-crawler/.env.example`에서 Redis 환경변수 제거
- **Convention Notes**: 파일 삭제 시 해당 파일을 참조하는 `__init__.py` 등도 확인
- **Verification**: 코드에서 redis 관련 import 없음 확인
- **Exit Criteria**: redis 관련 코드/의존성 완전 제거
- **Status**: completed

### Todo 3: infra Docker Compose Redis 제거
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Docker Compose에서 Redis 서비스, 볼륨, 의존성을 제거한다.
- **Work**:
  - `infra/docker-compose.yml`에서 `redis` 서비스 블록 삭제
  - `infra/docker-compose.yml`에서 `redis_data` 볼륨 삭제
  - `infra/docker-compose.yml`의 `main-server` 서비스에서 `redis` 의존성 및 REDIS 환경변수 제거
  - `infra/.env.example`에서 Redis 관련 환경변수 제거
- **Convention Notes**: YAML 들여쓰기 유지
- **Verification**: `docker-compose -f infra/docker-compose.yml config` 유효성 검증 (docker가 있는 경우)
- **Exit Criteria**: docker-compose에 redis 관련 내용 없음
- **Status**: completed

### Todo 4: Serena 메모리 업데이트
- **Priority**: 2
- **Dependencies**: Todo 1, 2, 3
- **Goal**: Serena 메모리에서 Redis 관련 내용을 업데이트한다.
- **Work**:
  - `project_overview.md` 메모리에서 Redis 관련 내용 제거/수정
  - `integration-test-guide.md` 메모리에서 Redis Testcontainer 관련 내용 제거
  - `e2e-testing-setup-complete.md` 메모리에서 Redis 관련 내용 제거
  - `e2e-testcontainers-refactored.md` 메모리에서 Redis 관련 내용 제거
- **Convention Notes**: 메모리 내용은 현재 코드 상태를 정확히 반영
- **Verification**: 메모리 읽기로 내용 확인
- **Exit Criteria**: 메모리가 현재 코드 상태를 정확히 반영
- **Status**: completed

### Todo 5: uv.lock 업데이트
- **Priority**: 2
- **Dependencies**: Todo 2
- **Goal**: pyproject.toml 변경 후 lock 파일을 재생성한다.
- **Work**:
  - `cd techtaurant-crawler && uv lock` 실행으로 lock 파일 재생성
- **Convention Notes**: uv가 설치되어 있어야 함
- **Verification**: `uv lock` 성공
- **Exit Criteria**: uv.lock에서 redis, hiredis, types-redis 패키지 제거됨
- **Status**: completed

## Verification Strategy
- `cd main-server && ./gradlew build` 빌드 및 테스트 통과
- 프로젝트 전체에서 `redis` import/의존성 검색으로 잔여 코드 없음 확인 (문서/스킬 파일 제외)

## Progress Tracking
- Total Todos: 5
- Completed: 5
- Status: ALL COMPLETE

## Change Log
- 2026-02-15: Plan created
- 2026-02-15: All todos completed
  - T1: main-server Redis 제거 + Caffeine 교체 + TokenCachePort 인터페이스 추출
  - T2: techtaurant-crawler Redis 제거 (모든 설정/예외/의존성)
  - T3: infra docker-compose Redis 서비스/볼륨/환경변수 제거
  - T4: Serena 메모리 Redis 참조 업데이트 (5개 메모리 파일)
  - T5: uv.lock 재생성 (redis, hiredis, types-redis 제거 확인)
