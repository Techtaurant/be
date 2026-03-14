# 게시물 목록 응답에 본문 일부 추가

## Business Goal
게시물 목록에서 사용자가 각 게시물의 내용을 일부 미리 확인할 수 있도록 본문 최대 2000자를 함께 제공하고, 해당 계약이 통합 테스트로 보장되도록 한다.

## Scope
- **In Scope**: `PostListItemResponse` 필드 추가, 목록 응답 매핑 로직 수정, 게시물 목록 조회 통합 테스트 추가
- **Out of Scope**: 게시물 상세 조회 API 변경, 데이터베이스 스키마 변경, 목록 외 API 응답 포맷 변경

## Codebase Analysis Summary
게시물 목록 응답은 `PostListReadService.convertToResponse()`에서 `PostListItemResponse`로 조립된다. 목록 API 통합 테스트는 `PostControllerTest`에 정렬, 페이지네이션, 기본 필드 검증 패턴으로 이미 존재하며 `IntegrationTest`를 기반으로 RestAssured를 사용한다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostListItemResponse.kt` | 게시물 목록 응답 DTO | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostListReadService.kt` | 목록 응답 매핑 서비스 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostControllerTest.kt` | 게시물 목록 API 통합 테스트 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO 변환 위치 | `.claude/core/BACKEND.md` 및 기존 구현 | 엔티티→응답 변환은 DTO/서비스 매핑 지점에서 처리 |
| 테스트 구조 | `.claude/framework/SPRING_BOOT.md` 및 기존 `PostControllerTest` | `IntegrationTest` 기반, Given-When-Then, 한글 `@DisplayName` 유지 |
| 구현 범위 최소화 | `.claude/core/CODE_PRINCIPLES.md` | 목록 응답에 필요한 필드만 추가하고 부가 기능은 넣지 않음 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 목록 본문 필드명 | `content` | 사용자 요청과 DTO 의미를 직접 반영 | `contentPreview` |
| 길이 제한 적용 위치 | `PostListReadService.convertToResponse()` | 기존 DTO 조립 지점에 응답 가공 책임이 집중됨 | DTO 내부 팩토리 메서드, DB 쿼리 단계 |
| 길이 제한 정책 | 최대 2000자, suffix 없음 | “2000자 정도” 요청을 단순하고 예측 가능하게 구현 | 2000자 + `...` |

## API Contracts (if applicable)

### `GET` `/open-api/posts`
- Headers: 없음
- Request: `sort`, `period`, `cursor`, `size` query param
- Response: `ApiResponse<CursorPageResponse<PostListItemResponse>>`
- Note: 각 `PostListItemResponse`는 `content` 필드로 게시물 본문 최대 2000자를 포함한다.

## Data Models (if applicable)

### `PostListItemResponse`
| Field | Type | Constraints |
|-------|------|-------------|
| `content` | `String` | 게시물 원문 기준 최대 2000자 |

## Implementation Todos

### Todo 1: 목록 응답 DTO와 매핑 로직에 본문 필드 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 게시물 목록 응답이 본문 최대 2000자를 포함하도록 구현한다.
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostListItemResponse.kt`에 `content` 필드를 추가한다.
  - `src/main/kotlin/com/techtaurant/mainserver/post/application/PostListReadService.kt`의 `convertToResponse()`에서 게시물 본문을 최대 2000자로 잘라 응답에 포함한다.
  - 기존 deprecated `from(post)` 팩토리도 동일 계약을 따르도록 보정한다.
- **Convention Notes**: 응답 가공 로직은 서비스/DTO 조립 경로에 두고, 필드명은 의미가 명확해야 한다.
- **Verification**: 관련 테스트 컴파일 통과, Kotlin 코드 빌드 오류 없음
- **Exit Criteria**: 목록 응답 DTO 생성 경로가 모두 `content`를 채우고 2000자 제한을 적용한다.
- **Status**: completed

### Todo 2: 게시물 목록 API 통합 테스트로 본문 포함 계약 검증
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 게시물 목록 조회 API가 본문을 포함하고 2000자 제한을 지키는지 검증한다.
- **Work**:
  - `src/test/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostControllerTest.kt`에 본문 포함 검증 테스트를 추가한다.
  - 2000자를 초과하는 게시물 본문과 짧은 본문을 각각 준비해 반환값을 검증한다.
  - Given-When-Then 주석과 한글 `@DisplayName`을 유지한다.
- **Convention Notes**: 기존 테스트 클래스 구조를 유지하고 RestAssured 응답 역직렬화 패턴을 재사용한다.
- **Verification**: 대상 테스트 실행 `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.in.PostControllerTest`
- **Exit Criteria**: 본문 포함/길이 제한 검증 테스트가 통과한다.
- **Status**: completed

## Verification Strategy
구현 후 게시물 목록 API 통합 테스트를 실행하여 DTO 직렬화와 서비스 매핑이 함께 검증되도록 한다.
- `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.in.PostControllerTest`

## Progress Tracking
- Total Todos: 2
- Completed: 2
- Status: Execution complete

## Change Log
- 2026-03-14: Plan created
- 2026-03-14: Todo 1 completed — 게시물 목록 응답 DTO와 매핑 로직에 content 필드 및 2000자 제한을 추가함
- 2026-03-14: Todo 2 completed — 게시물 목록 API 통합 테스트로 본문 포함 및 길이 제한 계약을 검증함
- 2026-03-14: Execution complete
