# 사용자 팔로우 API 추가

## Business Goal
사용자가 다른 사용자를 팔로우하거나 팔로우를 취소할 수 있게 하고, 특정 사용자의 팔로워 수와 팔로우 수 및 팔로워/팔로잉 목록을 조회할 수 있도록 하여 사용자 간 관계 기능을 제공한다. 또한 차단 시 기존 팔로우 관계를 즉시 정리해 관계 데이터 정합성을 유지한다.

## Scope
- **In Scope**: 사용자 팔로우/팔로우 취소 API, 특정 사용자 팔로워 수/팔로우 수 조회 API, 특정 사용자 팔로워 목록/팔로잉 목록 조회 API, 차단 시 양방향 팔로우 관계 자동 해제, Swagger 문서화, 통합 테스트
- **Out of Scope**: 팔로우 추천, 알림, isFollowing 상태 조회, 페이지네이션, 사용자 상세 프로필 API 신설

## Codebase Analysis Summary
현재 사용자 도메인은 `UserController`에서 인증이 필요한 내 정보/차단 쓰기 API를, `UserOpenApiController`에서 공개 조회 API를 담당한다. 차단 기능은 `UserBan`, `UserBanRepository`, `UserBanService` 구조로 구현되어 있으며 생성은 멱등적으로 처리되고 Swagger는 별도 Docs interface에 선언된다. 사용자 목록 조회 테스트는 RestAssured 기반 `IntegrationTest` 패턴을 따른다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/user/entity/UserFollow.kt` | 사용자 팔로우 관계 엔티티 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/out/UserFollowRepository.kt` | 팔로우 관계 조회/삭제 repository | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/UserFollowService.kt` | 팔로우/언팔로우/카운트/목록 비즈니스 로직 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/dto/UserFollowResponse.kt` | 팔로우 생성 응답 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/dto/UserFollowCountResponse.kt` | 팔로워/팔로우 수 응답 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/dto/UserFollowListItemResponse.kt` | 팔로워/팔로잉 목록 항목 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/enums/UserStatus.kt` | 팔로우 관련 에러 코드 추가 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/UserBanService.kt` | 차단 시 팔로우 관계 자동 해제 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserController.kt` | 팔로우/언팔로우 엔드포인트 추가 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserControllerDocs.kt` | 팔로우/언팔로우 Swagger 명세 추가 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserOpenApiController.kt` | 카운트/목록 조회 엔드포인트 추가 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserOpenApiControllerDocs.kt` | 카운트/목록 Swagger 명세 추가 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserControllerFollowIntegrationTest.kt` | 팔로우 API 통합 테스트 | Create |
| `src/test/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserControllerBanIntegrationTest.kt` | 차단 시 언팔로우 연동 테스트 보강 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Swagger 분리 | `CLAUDE.md`, `UserControllerDocs.kt` | Controller 구현체에는 Swagger 어노테이션을 두지 않고 `*ControllerDocs`에만 선언 |
| API 설명 한국어 | `.claude/core/BACKEND.md`, `CLAUDE.md` | summary, description, DTO 필드 설명을 모두 한국어로 작성 |
| 에러 코드 선언 | `CLAUDE.md`, `UserControllerDocs.kt` | 가능한 예외를 `ApiErrorCodeResponses`로 빠짐없이 선언 |
| 서비스 구조 | `UserBanService.kt`, `UserReadService.kt` | 사용자 도메인 비즈니스 로직은 application service에서 처리 |
| 테스트 스타일 | `integration-test-guide`, `UserControllerBanIntegrationTest.kt` | RestAssured 기반 통합 테스트, Given-When-Then 구분, 명시적 테스트 데이터 정리 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 팔로우 모델 | `UserFollow` 엔티티 별도 생성 | `UserBan`과 동일한 관계 모델로 정합성과 확장성을 확보 | 사용자 테이블에 카운트 컬럼 저장 |
| 팔로우 읽기/쓰기 서비스 | `UserFollowService` 단일 서비스 | 현재 `UserBanService`도 읽기/쓰기를 함께 담당해 구조 일관성이 높음 | Read/Write 서비스 분리 |
| 카운트/목록 API 위치 | `UserOpenApiController` | 공개 조회 API를 open-api에 두는 기존 패턴 준수 | `UserController`에 포함 |
| 차단 연동 방식 | `UserBanService`에서 팔로우 관계 삭제 | 차단 트랜잭션 내에서 정합성 있게 처리 가능 | DB cascade, 도메인 이벤트 |
| 차단 시 삭제 범위 | 양방향 팔로우 관계 모두 삭제 | 두 사용자 관계 단절이라는 요구를 가장 직접적으로 만족 | 한 방향만 삭제 |

## API Contracts

### POST /api/users/{targetUserId}/follow
- Headers: `Authorization: Bearer {accessToken}`
- Request: `없음`
- Response: `ApiResponse<UserFollowResponse>`
- Note: 본인 팔로우는 불가하며 대상 사용자가 없으면 에러를 반환한다.

### DELETE /api/users/{targetUserId}/follow
- Headers: `Authorization: Bearer {accessToken}`
- Request: `없음`
- Response: `204 No Content`
- Note: 팔로우 관계가 없으면 에러를 반환한다.

### GET /open-api/users/{userId}/follow-counts
- Headers: `없음`
- Request: `없음`
- Response: `ApiResponse<UserFollowCountResponse>`
- Note: 존재하지 않는 사용자는 에러를 반환한다.

### GET /open-api/users/{userId}/followings
- Headers: `없음`
- Request: `없음`
- Response: `ApiResponse<List<UserFollowListItemResponse>>`
- Note: 특정 사용자가 팔로우한 사용자 목록을 최신순으로 반환한다.

### GET /open-api/users/{userId}/followers
- Headers: `없음`
- Request: `없음`
- Response: `ApiResponse<List<UserFollowListItemResponse>>`
- Note: 특정 사용자를 팔로우하는 사용자 목록을 최신순으로 반환한다.

## Data Models

### UserFollow
| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `UUID` | PK |
| `follower` | `User` | FK, not null |
| `following` | `User` | FK, not null |
| `createdAt` | `Date` | 생성 시각 |
| `(follower_id, following_id)` | `unique` | 중복 팔로우 방지 |

## Implementation Todos

### Todo 1: 팔로우 도메인 모델과 서비스 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 팔로우 관계 저장과 조회를 처리할 수 있는 엔티티, repository, service, DTO, 에러 코드를 준비한다.
- **Work**:
  - `UserFollow` 엔티티와 `UserFollowRepository`를 추가한다.
  - `UserFollowService`에 팔로우, 언팔로우, 카운트 조회, 팔로워/팔로잉 목록 조회 로직을 구현한다.
  - `UserFollowResponse`, `UserFollowCountResponse`, `UserFollowListItemResponse` DTO를 추가한다.
  - `UserStatus`에 팔로우 관련 에러 코드를 추가한다.
- **Convention Notes**: `UserBan`과 유사한 관계형 엔티티 패턴을 따르고, DTO description은 한국어로 작성한다.
- **Verification**: 관련 코드 컴파일 확인, 서비스 로직에서 필요한 에러 케이스 점검
- **Exit Criteria**: 서비스 레벨에서 팔로우 관계 생성/삭제/카운트/목록 반환이 가능하다.
- **Status**: completed

### Todo 2: 컨트롤러와 Swagger 문서 추가
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 팔로우 쓰기 API와 공개 조회 API를 노출하고 Swagger 문서를 일치시킨다.
- **Work**:
  - `UserController`, `UserControllerDocs`에 팔로우/언팔로우 엔드포인트를 추가한다.
  - `UserOpenApiController`, `UserOpenApiControllerDocs`에 카운트/팔로워/팔로잉 조회 엔드포인트를 추가한다.
  - Swagger 응답 코드와 실제 HTTP status, 에러 코드 목록을 일치시킨다.
- **Convention Notes**: Controller와 Docs interface 시그니처를 동일하게 유지하고, 성공 응답은 content 없이 선언한다.
- **Verification**: 컨트롤러 시그니처와 Docs interface 일치 여부 확인
- **Exit Criteria**: 필요한 5개 API가 코드상 노출되고 Swagger 명세가 추가된다.
- **Status**: completed

### Todo 3: 차단 시 자동 언팔로우 연동 및 통합 테스트 작성
- **Priority**: 3
- **Dependencies**: Todo 1, Todo 2
- **Goal**: 차단 시 팔로우 관계가 자동 정리되고 API 동작이 통합 테스트로 보장되게 한다.
- **Work**:
  - `UserBanService`에서 차단 시 두 사용자 간 양방향 팔로우 관계를 삭제하도록 수정한다.
  - `UserControllerFollowIntegrationTest`를 추가해 팔로우/언팔로우/카운트/팔로워/팔로잉 조회를 검증한다.
  - `UserControllerBanIntegrationTest`에 차단 후 팔로우 관계 제거 검증을 추가한다.
- **Convention Notes**: RestAssured 통합 테스트 패턴과 명시적 데이터 정리 순서를 따른다.
- **Verification**: 사용자 follow/ban 관련 통합 테스트 실행
- **Exit Criteria**: 차단 연동과 신규 API가 테스트로 검증된다.
- **Status**: completed

### Todo 4: 포맷/검증 실행 및 마무리
- **Priority**: 4
- **Dependencies**: Todo 3
- **Goal**: 프로젝트 규칙에 맞게 포맷과 정적 검증을 완료하고 최종 상태를 정리한다.
- **Work**:
  - `./gradlew spotlessApply && ./gradlew spotlessCheck`를 실행한다.
  - 필요 시 실패 원인을 수정하고 재검증한다.
  - 계획 파일 진행 상태와 변경 로그를 완료 상태로 갱신한다.
- **Convention Notes**: `CLAUDE.md`의 완료 체크리스트를 그대로 따른다.
- **Verification**: `./gradlew spotlessApply && ./gradlew spotlessCheck`
- **Exit Criteria**: 포맷/검증 명령이 성공하고 계획 파일 상태가 최신이다.
- **Status**: completed

## Verification Strategy
전체 구현 후 팔로우/차단 관련 통합 테스트와 포맷 검증을 통해 동작과 코드 스타일을 함께 확인한다.
- `./gradlew test --tests "*UserControllerFollowIntegrationTest" --tests "*UserControllerBanIntegrationTest"`
- `./gradlew spotlessApply && ./gradlew spotlessCheck`

## Progress Tracking
- Total Todos: 4
- Completed: 4
- Status: Execution complete

## Change Log
- 2026-03-15: Plan created
- 2026-03-15: Todo 1 completed — 팔로우 도메인 모델, DTO, 서비스, 에러 코드 추가
- 2026-03-15: Todo 2 completed — 팔로우 컨트롤러와 Open API 및 Swagger 문서 추가
- 2026-03-15: Todo 3 completed — 차단 시 상호 팔로우 해제 연동과 팔로우 통합 테스트 추가
- 2026-03-15: Todo 4 completed — Spotless 포맷/검증 실행
- 2026-03-15: Execution complete
