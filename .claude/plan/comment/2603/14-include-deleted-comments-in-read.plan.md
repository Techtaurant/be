# 삭제 댓글 조회 포함 처리

## Business Goal
소프트 삭제된 댓글을 댓글 조회 API에서 계속 노출하여 클라이언트가 삭제 상태를 인지하고 스레드 구조를 유지할 수 있게 한다.

## Scope
- **In Scope**: 댓글 조회 경로에서 삭제 댓글 포함 처리, 목록 응답의 삭제 여부 노출, 조회 통합 테스트 검증
- **Out of Scope**: 댓글 삭제 정책 재설계, 삭제 API 계약 변경, 프론트엔드 표시 로직 변경

## Codebase Analysis Summary
댓글 조회는 `CommentReadService`가 `CommentRepositoryCustom`을 호출해 부모 댓글과 대댓글을 커서 기반으로 조회하는 구조다. 현재 `Comment` 엔티티에는 Hibernate `@Filter`가 자동 활성화되어 `deleted_at IS NULL` 조건이 기본 적용되므로, 커스텀 조회도 삭제 댓글을 자동 제외하고 있다. 댓글 응답 DTO는 생성/수정 응답용 `CommentResponse`와 목록 조회용 `CommentListResponse`로 분리되어 있으며, 통합 테스트는 RestAssured 기반 Given-When-Then 패턴을 따른다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/comment/entity/Comment.kt` | 댓글 엔티티와 soft delete 필터 정의 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/infrastructure/out/CommentRepositoryCustomImpl.kt` | 댓글 목록/대댓글 조회 커스텀 쿼리 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/dto/CommentListResponse.kt` | 댓글 목록 응답 DTO | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentReadService.kt` | 댓글 조회 서비스 | Reference |
| `src/test/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentReadControllerTest.kt` | 댓글 조회 통합 테스트 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO 변환은 Mapper/`from` 함수 사용 | `.claude/core/BACKEND.md` | 엔티티 → DTO 변환은 DTO companion `from`에서 처리 |
| 테스트는 Given-When-Then 구조 유지 | `.claude/framework/SPRING_BOOT.md`, `.serena/memories/integration-test-guide.md` | RestAssured 통합 테스트에 구간 주석과 한국어 `@DisplayName` 유지 |
| 최소 변경 원칙 | `.claude/core/CODE_PRINCIPLES.md` | soft delete 조회 요구만 해결하고 부수 추상화는 추가하지 않음 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 삭제 댓글 조회 방식 | 조회 커스텀 쿼리에서 Hibernate 필터를 일시 비활성화 | 현재 엔티티 soft delete 모델을 유지하면서 조회 API만 삭제 댓글 포함 가능 | `isDeleted` 컬럼 복구, 네이티브 쿼리 도입 |
| 삭제 상태 노출 | `CommentListResponse`에 `isDeleted` 추가 | 조회 응답에서 삭제 상태를 명시적으로 전달 가능 | 해시된 content만으로 간접 표현 |
| 검증 범위 | 조회 통합 테스트 클래스 중심 검증 | 실패 재현 지점이 명확하고 회귀 방지에 적합 | 전체 테스트 우선 실행 |

## API Contracts

### `GET /open-api/comments/posts/{postId}`
- Headers: 없음
- Request: `cursor`, `size`, `sort`, `userId`
- Response: `ApiResponse<CursorPageResponse<CommentListResponse>>`
- Note: 삭제 댓글도 포함하되 `isDeleted`로 상태를 전달

### `GET /open-api/comments/{commentId}/replies`
- Headers: 없음
- Request: `cursor`, `size`, `sort`, `userId`
- Response: `ApiResponse<CursorPageResponse<CommentListResponse>>`
- Note: 삭제 대댓글도 포함하되 `isDeleted`로 상태를 전달

## Data Models

### `Comment`
| Field | Type | Constraints |
|-------|------|-------------|
| `deletedAt` | `Date?` | soft delete 시각, `null`이면 활성 댓글 |

### `CommentListResponse`
| Field | Type | Constraints |
|-------|------|-------------|
| `isDeleted` | `Boolean` | `deletedAt != null` 기준 계산 |

## Implementation Todos

### Todo 1: 조회 쿼리에서 삭제 댓글 포함 처리
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 부모 댓글과 대댓글 조회 시 Hibernate soft delete 필터를 우회해 삭제 댓글도 읽을 수 있게 한다.
- **Work**:
  - `Comment.kt`에서 조회 구현체가 재사용할 수 있도록 필터 이름 상수를 노출한다.
  - `CommentRepositoryCustomImpl.kt`에서 Hibernate `Session`을 사용해 조회 실행 전 필터를 비활성화하고, 조회 후 원상 복구한다.
  - 기존 정렬/커서 로직은 변경하지 않는다.
- **Convention Notes**: 커스텀 쿼리 구현체 내부에서만 정책을 바꾸고 서비스 시그니처는 유지한다.
- **Verification**: `./gradlew test --tests com.techtaurant.mainserver.comment.infrastructure.in.CommentReadControllerTest`
- **Exit Criteria**: 삭제된 부모 댓글/대댓글이 조회 결과에 포함되어 실패 테스트가 통과할 기반이 마련된다.
- **Status**: completed

### Todo 2: 목록 응답에 삭제 여부 반영
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 삭제 댓글을 조회 결과에 포함할 때 클라이언트가 상태를 구분할 수 있도록 목록 응답에 삭제 여부를 담는다.
- **Work**:
  - `CommentListResponse.kt`에 `isDeleted` 필드를 추가한다.
  - `CommentListResponse.from(...)`에서 `comment.deletedAt != null` 기준으로 값을 매핑한다.
  - `CommentReadControllerTest.kt`에서 삭제 댓글 조회 케이스에 `isDeleted == true` 검증을 추가한다.
- **Convention Notes**: 필드명은 Boolean 네이밍 규칙에 맞춰 `isDeleted`를 사용한다.
- **Verification**: `./gradlew test --tests com.techtaurant.mainserver.comment.infrastructure.in.CommentReadControllerTest`
- **Exit Criteria**: 조회 응답이 삭제 여부를 명시하고 테스트에서 이를 검증한다.
- **Status**: completed

### Todo 3: 포맷 및 회귀 검증
- **Priority**: 3
- **Dependencies**: Todo 1, Todo 2
- **Goal**: 변경 사항이 스타일 규칙과 빌드 검증을 통과하는지 확인한다.
- **Work**:
  - `./gradlew spotlessApply` 실행
  - `./gradlew spotlessCheck` 실행
  - `./gradlew test --tests com.techtaurant.mainserver.comment.infrastructure.in.CommentReadControllerTest` 재실행
- **Convention Notes**: 완료 체크리스트에 맞춰 포맷 검증을 반드시 포함한다.
- **Verification**: 위 명령들의 성공 여부 확인
- **Exit Criteria**: 포맷 검사와 대상 테스트가 모두 통과한다.
- **Status**: completed

## Verification Strategy
조회 통합 테스트를 먼저 통과시켜 soft delete 포함 동작을 검증하고, 이후 spotless 적용/검사로 스타일 규칙까지 확인한다.
- `./gradlew test --tests com.techtaurant.mainserver.comment.infrastructure.in.CommentReadControllerTest`
- `./gradlew spotlessApply`
- `./gradlew spotlessCheck`

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-03-14: Plan created
- 2026-03-14: Todo 1 completed — 조회 커스텀 쿼리에서 soft delete 필터를 일시 비활성화해 삭제 댓글을 포함하도록 수정
- 2026-03-14: Todo 2 completed — 목록 응답에 isDeleted를 추가하고 삭제 댓글 조회 테스트에서 상태를 검증하도록 보강
- 2026-03-14: Todo 3 completed — spotless 적용과 조회 통합 테스트 재검증 완료
- 2026-03-14: Execution complete
