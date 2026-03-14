# Add Post Count To Category List

## Business Goal
카테고리 목록 조회 API에서 각 카테고리에 연결된 게시물 개수를 함께 반환하여, 클라이언트가 추가 조회 없이 카테고리별 게시물 규모를 바로 표시할 수 있도록 한다.

## Scope
- **In Scope**: 카테고리 목록 응답 DTO에 게시물 개수 필드 추가, 카테고리별 게시물 수 일괄 집계 쿼리 추가, Swagger 반영, 관련 테스트 추가
- **Out of Scope**: 게시물 목록 자체 반환, 하위 카테고리 누적 게시물 수 계산, 다른 게시물 조회 API 변경

## Codebase Analysis Summary
카테고리 목록 API는 `CategoryReadController`가 `CategoryReadService.searchByPath`를 호출하고, 서비스는 `CategoryRepository`로 카테고리 엔티티를 조회한 뒤 `CategoryResponse.from`으로 DTO 변환한다. 현재 게시물 저장 구조는 `Post.category`에 직접 연결되는 형태이며, 카테고리 목록 응답에는 게시물 관련 필드가 없다. 따라서 카테고리 목록을 먼저 조회한 다음 카테고리 ID 기준으로 게시물 수를 일괄 집계해서 DTO에 주입하는 방식이 기존 구조와 가장 잘 맞는다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/CategoryReadController.kt | 카테고리 목록 API 엔트리포인트 | Reference |
| src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/CategoryReadControllerDocs.kt | Swagger 문서 정의 | Modify |
| src/main/kotlin/com/techtaurant/mainserver/post/application/CategoryReadService.kt | 카테고리 목록 조회 서비스 | Modify |
| src/main/kotlin/com/techtaurant/mainserver/post/dto/CategoryResponse.kt | 카테고리 응답 DTO | Modify |
| src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepository.kt | 게시물 저장소 | Modify |
| src/test/kotlin/com/techtaurant/mainserver/post/application/CategoryReadServiceTest.kt | 카테고리 목록 응답 검증 테스트 | Create |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO 명명 | `.claude/core/BACKEND.md` | 응답 DTO는 `Response` 접미사를 유지하고 필드 설명을 Swagger에 명시한다 |
| 구현 단순성 | `.claude/core/CODE_PRINCIPLES.md` | 요구사항 범위만 구현하고 불필요한 추상화나 확장은 추가하지 않는다 |
| Spring Boot 테스트 | `.claude/framework/SPRING_BOOT.md` | Given-When-Then 구조와 한글 `@DisplayName`을 유지한다 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 응답 확장 방식 | `CategoryResponse`에 `postCount` 추가 | 현재 API 계약을 최소 변경으로 확장 가능 | 별도 카테고리 목록 전용 DTO 생성 |
| 게시물 수 조회 방식 | 카테고리 ID 목록 기준 일괄 count 조회 | N+1 없이 서비스 레이어에서 매핑 가능 | 카테고리별 개별 count 조회 |
| count 의미 | 카테고리에 직접 연결된 게시물 수 | 현재 `Post.category` 모델과 요구사항에 정확히 부합 | 하위 카테고리 포함 누적 수 |

## API Contracts

### GET /open-api/users/{userId}/categories
- Headers: 없음
- Request: `path` 쿼리 파라미터는 선택
- Response: `ApiResponse<List<CategoryResponse>>` (`CategoryResponse.postCount` 추가)
- Note: `postCount`는 각 카테고리에 직접 연결된 게시물 개수다

## Data Models

### CategoryResponse
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | 카테고리 ID |
| name | String | nullable 아님 |
| path | String | nullable 아님 |
| depth | Int | 1~5 |
| parentId | UUID? | 최상위 카테고리면 null |
| postCount | Long | 0 이상 |

## Implementation Todos

### Todo 1: 계획 파일 생성 및 실행 준비
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 작업 계획을 저장하고 실행 기준을 고정한다
- **Work**:
  - `.claude/plan/post/2603/13-add-post-count-to-category-list.plan.md` 생성
  - 변경 대상 파일과 검증 전략을 명시
- **Convention Notes**: 계획은 한국어로 작성하고 실제 변경 범위만 포함한다
- **Verification**: 계획 파일 존재 여부 확인
- **Exit Criteria**: 계획 파일이 저장되고 Todo 상태 관리가 가능하다
- **Status**: completed

### Todo 2: 카테고리 응답에 게시물 수 반영
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 카테고리 목록 API가 카테고리별 게시물 개수를 반환하도록 구현한다
- **Work**:
  - `PostRepository`에 카테고리 ID 목록 기준 게시물 수 집계 쿼리 추가
  - `CategoryReadService.searchByPath`에서 카테고리 조회 후 게시물 수 맵 생성
  - `CategoryResponse`에 `postCount` 필드와 변환 함수 반영
  - `CategoryReadControllerDocs` 설명을 응답 구조에 맞게 갱신
- **Convention Notes**: `from` 변환 패턴을 유지하고 서비스 책임을 불필요하게 늘리지 않는다
- **Verification**: 관련 Kotlin 코드 컴파일 가능 여부 확인
- **Exit Criteria**: 카테고리 목록 응답 DTO에 `postCount`가 채워지는 코드가 반영된다
- **Status**: completed

### Todo 3: 테스트 추가 및 검증
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 게시물 수 계산과 응답 매핑이 의도대로 동작하는지 검증한다
- **Work**:
  - `CategoryReadServiceTest`를 생성해 카테고리별 게시물 수 매핑을 검증
  - 카테고리가 있지만 게시물이 없는 경우 `0`을 반환하는 케이스 포함
  - 필요한 테스트 명령 실행
- **Convention Notes**: Given-When-Then 구조와 한글 테스트명을 사용한다
- **Verification**: 대상 테스트 실행
- **Exit Criteria**: 새 테스트가 통과하고 요구사항을 보호한다
- **Status**: completed

## Verification Strategy
구현 후 서비스 테스트와 대상 Gradle 테스트를 실행해 카테고리별 게시물 수가 정상 매핑되는지 확인한다.
- `./gradlew test --tests com.techtaurant.mainserver.post.application.CategoryReadServiceTest`
- 가능하면 컴파일과 테스트를 통해 DTO/Repository 변경 영향도 확인

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-03-13: Plan created
- 2026-03-13: Todo 1 completed — 계획 파일 생성 및 실행 기준 확정
- 2026-03-13: Todo 2 completed — 카테고리 응답에 게시물 수 집계 및 Swagger 반영
- 2026-03-13: Todo 3 completed — CategoryReadService 테스트 추가 및 검증 완료
- 2026-03-13: Execution complete
