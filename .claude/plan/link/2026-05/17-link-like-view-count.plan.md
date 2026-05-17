# Link Like View Count API

## Business Goal
링크 콘텐츠에도 게시글과 동일하게 사용자 좋아요 상태와 조회 이벤트를 기록하고, 링크 목록에서 최신 조회수와 좋아요수를 제공해 프론트엔드가 링크 반응 지표를 표시할 수 있게 한다.

## Scope
- **In Scope**: links 테이블 카운트 컬럼 추가, link view/like 로그 테이블 및 엔티티/리포지토리 추가, 링크 좋아요 API, 링크 조회 로그 API, 링크 목록 응답 카운트 필드, 관련 통합 테스트.
- **Out of Scope**: 링크 일별 통계 테이블, 링크 상세 조회 API, 기존 post/comment 상호작용 로직 변경, 프론트엔드 변경.

## Codebase Analysis Summary
post 도메인은 `PostLikeLogService`와 `PostViewLogService`가 로그 저장 후 `PostRepository` 원자적 update 쿼리로 카운트를 갱신한다. link 도메인은 현재 저장/읽음 API만 있고 `Link` 엔티티와 `links` 테이블에는 카운트 컬럼이 없다. link 컨트롤러는 `/api` 아래 인증 API를 모아두고, post 조회 로그는 `/open-api/posts/{postId}/view-logs`에서 비로그인 조회도 기록한다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/resources/db/migration/V29__add_link_interaction_counts.sql` | link 카운트/로그 스키마 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/entity/Link.kt` | 링크 엔티티 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/link/entity/LinkLikeLog.kt` | 링크 좋아요 로그 엔티티 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/entity/LinkViewLog.kt` | 링크 조회 로그 엔티티 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/out/LinkRepository.kt` | 링크 카운트 update 쿼리 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/out/LinkLikeLogRepository.kt` | 링크 좋아요 로그 저장소 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/out/LinkViewLogRepository.kt` | 링크 조회 로그 저장소 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/application/LinkLikeLogService.kt` | 링크 좋아요 상태 처리 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/application/LinkViewLogService.kt` | 링크 조회 로그 처리 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/dto/RecordLinkLikeRequest.kt` | 링크 좋아요 요청 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/dto/LinkListItemResponse.kt` | 링크 목록 응답 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/in/LinkController.kt` | 인증 링크 API | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/in/LinkControllerDocs.kt` | Swagger 문서 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/in/LinkViewLogOpenApiController.kt` | 공개 조회 로그 API | Create |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/in/LinkViewLogOpenApiControllerDocs.kt` | 공개 조회 로그 Swagger 문서 | Create |
| `src/test/kotlin/com/techtaurant/mainserver/link/infrastructure/in/LinkControllerIntegrationTest.kt` | link API 통합 테스트 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/link/application/LinkLikeLogServiceTest.kt` | link 좋아요 서비스 검증 | Create |
| `src/test/kotlin/com/techtaurant/mainserver/link/application/LinkViewLogServiceTest.kt` | link 조회 서비스 검증 | Create |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| API 응답 | `LinkController.kt`, `PostLikeController.kt` | 성공 응답은 `ApiResponse.ok(Unit)` 또는 생성/삭제 상태에 맞춘 기존 패턴 사용 |
| 좋아요 상태 | `PostLikeLogService.kt`, `CommentLikeLogService.kt` | `LikeStatus`의 NONE/LIKE/DISLIKE를 재사용하고 기존 로그의 상태 전이에 따라 카운트 증감 |
| 조회 로그 | `PostViewLogService.kt`, `PostViewLogOpenApiController.kt` | 비로그인 사용자도 허용하고 IP/User-Agent를 로그에 저장 |
| 카운트 갱신 | `PostRepository.kt` | JPA `@Modifying` update 쿼리로 원자적 증가/감소 처리 |
| 테스트 | `LinkControllerIntegrationTest.kt`, post 서비스 테스트 | Testcontainers 기반 통합 테스트와 RestAssured 패턴 유지 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 링크 좋아요 모델 | `LikeStatus`와 `is_liked` 로그 재사용 | post/comment와 사용자 상호작용 규칙이 같아 API/FE 이해 비용이 낮다 | Boolean toggle 전용 API |
| 조회 로그 API 위치 | `/open-api/links/{linkId}/view-logs` | post 조회 로그 API와 동일하게 비로그인 조회 기록을 지원한다 | 인증 필요 `/api/links/{linkId}/view-logs` |
| 링크 통계 범위 | `links.view_count`, `links.like_count`만 유지 | 요청 범위는 카운트 증가 API이며 link 일별 통계 인프라는 없다 | link daily stats 신규 도입 |
| 링크 목록 응답 | `viewCount`, `likeCount` 필드 추가 | 카운트 API 결과를 기존 링크 목록에서 바로 표시할 수 있다 | 별도 metadata API 추가 |

## API Contracts

### POST `/api/links/{linkId}/like`
- Headers: `Authorization: Bearer {accessToken}`
- Request: `{"likeStatus":"LIKE|DISLIKE|NONE"}`
- Response: `ApiResponse<Unit>`
- Note: 인증 사용자만 호출 가능. `NONE`은 기존 로그를 삭제하고 카운트를 되돌린다.

### POST `/open-api/links/{linkId}/view-logs`
- Headers: optional `Authorization: Bearer {accessToken}`
- Request: none
- Response: `ApiResponse<Unit>`
- Note: 비로그인 조회도 기록하며 IP 주소와 User-Agent를 저장한다.

## Data Models

### Link
| Field | Type | Constraints |
|-------|------|-------------|
| `viewCount` | `Long` | `links.view_count`, NOT NULL DEFAULT 0 |
| `likeCount` | `Long` | `links.like_count`, NOT NULL DEFAULT 0 |

### LinkLikeLog
| Field | Type | Constraints |
|-------|------|-------------|
| `link` | `Link` | FK `link_id`, NOT NULL |
| `user` | `User` | FK `user_id`, NOT NULL |
| `isLiked` | `Boolean` | NOT NULL DEFAULT TRUE |
| `(link_id, user_id)` | unique | 한 사용자당 마지막 좋아요 상태만 유지 |

### LinkViewLog
| Field | Type | Constraints |
|-------|------|-------------|
| `link` | `Link` | FK `link_id`, NOT NULL |
| `user` | `User?` | nullable, 비회원 조회 지원 |
| `ipAddress` | `String?` | VARCHAR(45) |
| `userAgent` | `String?` | TEXT |

## Implementation Todos

### Todo 1: Schema and Persistence Model
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 링크 카운트와 로그를 저장할 DB/JPA 모델을 준비한다.
- **Work**:
  - `V29__add_link_interaction_counts.sql` 생성.
  - `Link.kt`에 `viewCount`, `likeCount` 추가.
  - `LinkLikeLog.kt`, `LinkViewLog.kt` 생성.
  - `LinkLikeLogRepository.kt`, `LinkViewLogRepository.kt` 생성.
  - `LinkRepository.kt`에 `incrementViewCount`, `incrementLikeCount`, `decrementLikeCount` 추가.
- **Convention Notes**: post의 로그 테이블 인덱스와 `@Modifying(clearAutomatically = false, flushAutomatically = true)` 패턴을 따른다.
- **Verification**: Kotlin compile 대상 파일 구조와 Flyway migration 명명 규칙 확인.
- **Exit Criteria**: link 카운트/로그 모델이 컴파일 가능한 상태로 존재한다.
- **Status**: completed

### Todo 2: Application Services and API
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 링크 좋아요 상태와 조회 로그를 기록하는 API를 제공한다.
- **Work**:
  - `RecordLinkLikeRequest.kt` 생성.
  - `LinkLikeLogService.kt` 생성.
  - `LinkViewLogService.kt` 생성.
  - `LinkController.kt`, `LinkControllerDocs.kt`에 `POST /api/links/{linkId}/like` 추가.
  - `LinkViewLogOpenApiController.kt`, `LinkViewLogOpenApiControllerDocs.kt` 생성.
- **Convention Notes**: `LinkStatus.LINK_NOT_FOUND`, `UserStatus.ID_NOT_FOUND`, `HttpRequestUtils.extractIpAddress`를 사용한다.
- **Verification**: 서비스와 컨트롤러 시그니처가 기존 Spring 패턴과 일치하는지 확인한다.
- **Exit Criteria**: 좋아요/조회 API가 서비스까지 연결된다.
- **Status**: completed

### Todo 3: Responses and Tests
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 링크 목록에서 카운트를 노출하고 핵심 동작을 자동 검증한다.
- **Work**:
  - `LinkListItemResponse.kt`에 `viewCount`, `likeCount` 추가.
  - `LinkControllerIntegrationTest.kt`에 좋아요 API 및 목록 카운트 검증 추가.
  - `LinkLikeLogServiceTest.kt`, `LinkViewLogServiceTest.kt` 작성.
- **Convention Notes**: 기존 RestAssured와 AssertJ 통합 테스트 스타일을 유지한다.
- **Verification**: link 관련 테스트와 Gradle compile/test 실행.
- **Exit Criteria**: 신규 API와 카운트 갱신이 테스트로 검증된다.
- **Status**: completed

## Verification Strategy
- `./gradlew test --tests "com.techtaurant.mainserver.link.*"`
- 필요 시 `./gradlew compileKotlin compileTestKotlin`
- 실패 시 원인 수정 후 동일 범위 재실행

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-05-17: Plan created
- 2026-05-17: Todo 1 completed - Schema and persistence model added
- 2026-05-17: Todo 2 completed - Application services and API added
- 2026-05-17: Todo 3 completed - Responses and tests added
- 2026-05-17: Execution complete - Link like/view count API verified
