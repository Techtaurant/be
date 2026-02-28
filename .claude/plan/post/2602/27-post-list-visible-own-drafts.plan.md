# 게시물 전체 목록에서 본인 DRAFT/PRIVATE 게시물 노출

## Business Goal
게시물 전체 목록 조회 API에서 로그인한 사용자가 자신의 DRAFT/PRIVATE 게시물도 함께 볼 수 있도록 한다. 타인의 DRAFT/PRIVATE은 여전히 비노출.

## Scope
- **In Scope**: `getPosts()` 서비스 로직 수정, Repository 쿼리 조건 추가, Swagger 에러 응답 명세 보강
- **Out of Scope**: `getPostsByUserId()` (이미 구현됨), 새로운 API 엔드포인트 추가

## Codebase Analysis Summary
`PostListReadService.getPosts()`가 `findPostsWithConditions()`를 statuses 파라미터 없이 호출하여 PUBLISHED만 조회됨. Repository는 이미 `statuses`, `authorId` 파라미터를 지원하지만 "PUBLISHED OR 내 게시물" 같은 OR 조건은 불가. 새 파라미터 필요.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `PostRepositoryCustom.kt` | 커스텀 Repository 인터페이스 | Modify - `visibleToUserId` 파라미터 추가 |
| `PostRepositoryCustomImpl.kt` | 커스텀 Repository 구현체 | Modify - OR 조건 쿼리 추가 |
| `PostListReadService.kt` | 게시물 목록 서비스 | Modify - `getPosts()`에서 currentUserId 전달 |
| `PostReadOpenApiController.kt` | Open API 컨트롤러 | Modify - Swagger 400 에러 응답 스키마 추가 |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Swagger 에러 응답 | CLAUDE.md | `@Content` + `@Schema` + `@ExampleObject` 필수, const val 분리 |
| Given-When-Then 테스트 | SPRING_BOOT.md | 모든 테스트는 GWT 패턴 준수 |
| spotless | CLAUDE.md | 변경 후 `spotlessApply && spotlessCheck` 필수 |

## Implementation Todos

### Todo 1: Repository에 visibleToUserId 파라미터 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: `findPostsWithConditions()`에 `visibleToUserId` 파라미터를 추가하여 `(status = PUBLISHED) OR (author = visibleToUserId)` 조건 지원
- **Work**:
  - `PostRepositoryCustom.kt`: `visibleToUserId: UUID? = null` 파라미터 추가
  - `PostRepositoryCustomImpl.kt`: `visibleToUserId != null`일 때 `cb.or(publishedPredicate, authorPredicate)` 조건 생성. 기존 `statuses`/기본 PUBLISHED 로직보다 우선 적용
- **Convention Notes**: 기존 `authorId`, `statuses`, `categoryId`와 동일한 nullable 기본값 패턴
- **Verification**: 컴파일 성공
- **Exit Criteria**: `findPostsWithConditions(visibleToUserId = someUUID)` 호출 시 PUBLISHED + 해당 사용자의 모든 상태 게시물 반환
- **Status**: completed

### Todo 2: 서비스 로직 수정
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: `PostListReadService.getPosts()`에서 현재 로그인 사용자 ID를 `visibleToUserId`로 전달
- **Work**:
  - `PostListReadService.kt`의 `getPosts()` 메서드에서 `getCurrentUserId()`를 `visibleToUserId` 파라미터로 전달
- **Convention Notes**: 기존 `getCurrentUserId()` 메서드 재사용
- **Verification**: 컴파일 성공
- **Exit Criteria**: 로그인 시 본인 DRAFT/PRIVATE 포함, 비로그인 시 PUBLISHED만 조회
- **Status**: completed

### Todo 3: Swagger 에러 응답 명세 보강
- **Priority**: 1
- **Dependencies**: none
- **Goal**: `PostReadOpenApiController`의 400 에러 응답에 실제 응답 포맷 문서화
- **Work**:
  - `PostReadOpenApiController.kt`에 `@Content` + `@Schema(implementation = ApiResponse::class)` + `@ExampleObject` 추가
  - `companion object`에 `VALIDATION_ERROR_EXAMPLE` const val 정의
  - import alias `import ... as SwaggerApiResponse` 적용
- **Convention Notes**: CLAUDE.md의 Swagger 에러 응답 명세 규칙 준수
- **Verification**: 컴파일 성공, spotlessCheck 통과
- **Exit Criteria**: Swagger UI에서 400 에러 응답 body 스키마와 예시 확인 가능
- **Status**: completed

### Todo 4: 테스트 추가
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 2
- **Goal**: 변경된 로직에 대한 테스트 커버리지 확보
- **Work**:
  - `PostListReadServiceTest.kt`에 `getPosts()` 관련 테스트 추가:
    - 로그인 사용자: 본인 DRAFT/PRIVATE 포함 + 타인 PUBLISHED만 조회 (visibleToUserId 전달 확인)
    - 비로그인: visibleToUserId가 null로 전달 확인
  - `PostRepositoryCustomImplTest.kt`에 `visibleToUserId` 테스트 추가:
    - visibleToUserId 지정 시 PUBLISHED + 해당 사용자의 DRAFT/PRIVATE 모두 반환
    - visibleToUserId null 시 PUBLISHED만 반환
- **Convention Notes**: MockK, Given-When-Then, @DisplayName 한국어
- **Verification**: 테스트 전체 통과
- **Exit Criteria**: 모든 분기 커버리지 확보
- **Status**: completed

## Verification Strategy
- `./gradlew spotlessApply && ./gradlew spotlessCheck`
- `./gradlew test --tests PostListReadServiceTest`
- `./gradlew test --tests PostRepositoryCustomImplTest`

## Progress Tracking
- Total Todos: 4
- Completed: 4
- Status: All completed

## Change Log
- 2026-02-27: Plan created
- 2026-02-27: All todos completed. 20 tests passed (9 unit + 11 integration), spotless check passed
