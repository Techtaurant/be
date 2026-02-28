# User Posts List API

## Business Goal
특정 사용자의 게시물 목록을 조회하는 API를 제공한다. 본인의 게시물 조회 시 DRAFT/PRIVATE 포함, 타인 조회 시 PUBLISHED만 반환. categoryId 필터링 지원.

## Scope
- **In Scope**: PostRepositoryCustom에 authorId/statuses/categoryId 파라미터 추가, PostListReadService에 사용자별 조회 메서드, UserOpenApiController에 엔드포인트, Swagger 문서화
- **Out of Scope**: 새 DTO 생성 (PostListItemResponse 재사용), 테스트 코드

## Codebase Analysis Summary
- Post 엔티티는 `author: User` (ManyToOne) 관계
- `PostRepositoryCustomImpl.findPostsWithConditions`가 Criteria API로 동적 쿼리 수행 (기간, 정렬, 커서)
- `PostListReadService.getPosts`가 커서 페이지네이션 + 읽음 상태 처리
- `UserOpenApiController`가 `/open-api/users` 경로의 공개 API 담당
- `getCurrentUserId()`로 SecurityContext에서 현재 사용자 ID 추출 (비회원이면 null)

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `post/infrastructure/out/PostRepositoryCustom.kt` | 커스텀 쿼리 인터페이스 | Modify |
| `post/infrastructure/out/PostRepositoryCustomImpl.kt` | 커스텀 쿼리 구현체 | Modify |
| `post/application/PostListReadService.kt` | 게시물 목록 조회 서비스 | Modify |
| `user/infrastructure/in/UserOpenApiController.kt` | 사용자 Open API 컨트롤러 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Open API 경로 | `SecurityConstants.OPEN_API_PREFIX` | `/open-api/` prefix 사용 |
| 응답 래핑 | 기존 컨트롤러 | `ApiResponse.ok()` 사용 |
| 페이지네이션 | `CursorPageResponse<T>` | 커서 기반 페이지네이션 |
| Swagger | 기존 컨트롤러 | `@Operation`, `@ApiResponses`, `@Parameter` 어노테이션 |
| 쿼리 파라미터 | 기존 `getPosts` | `@RequestParam(required = false)` 패턴 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 엔드포인트 위치 | `UserOpenApiController` | User 리소스 하위 경로 | PostReadOpenApiController |
| 상태 필터 로직 | Service에서 본인 여부 판단 | 비즈니스 로직은 서비스 레이어 | Controller 분기 |
| 쿼리 변경 | 기존 findPostsWithConditions에 optional 파라미터 추가 | DRY | 별도 메서드 |
| 응답 DTO | PostListItemResponse 재사용 | 동일 구조 | 새 DTO |

## API Contracts

### GET /open-api/users/{userId}/posts
- Headers: Authorization (optional, Bearer JWT)
- Request Query Params:
  - `cursor`: String? (이전 응답의 nextCursor)
  - `size`: Int (1-100, 기본값 20)
  - `period`: PostPeriod (WEEK/MONTH/YEAR/ALL, 기본값 ALL)
  - `sort`: PostSortType (LATEST/VIEW/LIKE/COMMENT, 기본값 LATEST)
  - `categoryId`: UUID? (카테고리 필터)
- Response: `ApiResponse<CursorPageResponse<PostListItemResponse>>`
- Note: 본인 조회 시 모든 상태, 타인 조회 시 PUBLISHED만

## Implementation Todos

### Todo 1: PostRepositoryCustom/Impl에 authorId, statuses, categoryId 파라미터 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 동적 쿼리에 작성자, 상태 목록, 카테고리 필터 조건 추가
- **Work**:
  - `PostRepositoryCustom.findPostsWithConditions`에 `authorId: UUID? = null`, `statuses: List<PostStatusEnum>? = null`, `categoryId: UUID? = null` 파라미터 추가
  - `PostRepositoryCustomImpl.findPostsWithConditions`에 해당 파라미터 반영
  - authorId가 non-null이면 `cb.equal(root.get(Post_.AUTHOR).get("id"), authorId)` 조건 추가
  - statuses가 non-null이면 기존 `cb.equal(root.get(Post_.status), PostStatusEnum.PUBLISHED)` 대신 `root.get(Post_.status).in(statuses)` 사용
  - statuses가 null이면 기존대로 PUBLISHED만 필터
  - categoryId가 non-null이면 `cb.equal(root.get(Post_.CATEGORY).get("id"), categoryId)` 조건 추가
  - 기존 호출부 (`PostListReadService.getPosts`)는 파라미터 기본값으로 영향 없음
- **Convention Notes**: Criteria API 패턴 유지, 기존 predicate 추가 방식 준수
- **Verification**: 빌드 성공 (`./gradlew compileKotlin`)
- **Exit Criteria**: 기존 테스트 통과, 새 파라미터가 optional이어서 기존 호출 영향 없음
- **Status**: pending

### Todo 2: PostListReadService에 사용자별 게시물 조회 메서드 추가
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 사용자 ID 기반 게시물 목록 조회 + 본인 여부에 따른 상태 필터 로직
- **Work**:
  - `PostListReadService`에 `getPostsByUserId(userId: UUID, cursor: String?, size: Int, period: PostPeriod, sortType: PostSortType, categoryId: UUID?)` 메서드 추가
  - `getCurrentUserId()`로 현재 사용자 확인
  - `currentUserId == userId`이면 statuses = `listOf(DRAFT, PUBLISHED, PRIVATE)`, 아니면 statuses = `listOf(PUBLISHED)`
  - `postRepository.findPostsWithConditions(cursor, size+1, period, sortType, authorId=userId, statuses=statuses, categoryId=categoryId)` 호출
  - 이후 기존 `getPosts`와 동일한 커서/읽음 처리 로직 적용
  - `getCurrentUserId()` 메서드를 private에서 접근 가능하도록 조정 (이미 같은 클래스 내부이므로 그대로 사용)
- **Convention Notes**: 기존 `getPosts` 메서드의 패턴 (커서 디코딩 → 쿼리 → hasNext 계산 → 읽음 상태 → 응답 변환) 동일하게 따름
- **Verification**: 빌드 성공
- **Exit Criteria**: 메서드가 정상 컴파일, 기존 getPosts 영향 없음
- **Status**: pending

### Todo 3: UserOpenApiController에 엔드포인트 추가
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: GET /open-api/users/{userId}/posts 엔드포인트 추가 + Swagger 문서화
- **Work**:
  - `UserOpenApiController`에 `PostListReadService` 의존성 추가 (생성자 주입)
  - `getPostsByUserId` 메서드 추가:
    - `@GetMapping("/{userId}/posts")`
    - `@PathVariable userId: UUID`
    - `@RequestParam cursor, size, period, sort, categoryId` (기존 PostReadOpenApiController.getPosts와 동일 패턴)
    - `@Operation`, `@ApiResponses`, `@Parameter` Swagger 어노테이션
  - `PostListReadService.getPostsByUserId()` 호출하여 결과 반환
- **Convention Notes**: 기존 `PostReadOpenApiController.getPosts`의 Swagger 패턴 참고, `@Tag(name = "User")` 유지
- **Verification**: 빌드 성공 + spotlessCheck 통과
- **Exit Criteria**: 엔드포인트 정상 등록, Swagger 문서 생성
- **Status**: pending

## Verification Strategy
- `cd main-server && ./gradlew spotlessApply && ./gradlew spotlessCheck`
- `cd main-server && ./gradlew compileKotlin`

## Progress Tracking
- Total Todos: 3
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-27: Plan created
