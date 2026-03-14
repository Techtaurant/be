# 댓글 삭제 시 content 해시 치환

## Business Goal
댓글 삭제 시 원문을 고정 문구로 덮어쓰는 대신 해시 문자열로 치환해 원문 노출을 방지하면서도 soft delete 상태를 유지하도록 구현한다.

## Scope
- **In Scope**: 댓글 삭제 로직을 서비스 레이어에서 해시 기반 soft delete로 변경, 관련 엔티티 상수와 주석 정리, 검증 수행
- **Out of Scope**: 응답 DTO 구조 변경, 추가 DB 컬럼 도입, 삭제 이력 별도 저장

## Codebase Analysis Summary
현재 댓글 삭제는 `CommentDeleteService`에서 권한 검증 후 `commentRepository.delete(comment)`를 호출하고, 실제 블라인드 처리는 `Comment` 엔티티의 `@SQLDelete`에서 고정 문구로 수행한다. 사용자가 요구한 "원문 해시 치환"은 SQL 상수 치환으로는 표현력이 부족하므로, 서비스 레이어에서 해시를 계산해 `content`, `isDeleted`, `deletedAt`을 직접 갱신하는 방식이 적합하다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentDeleteService.kt | 댓글 삭제 유스케이스 | Modify |
| src/main/kotlin/com/techtaurant/mainserver/comment/entity/Comment.kt | 댓글 soft delete 상태 필드와 상수 정의 | Modify |
| src/main/kotlin/com/techtaurant/mainserver/comment/dto/CommentResponse.kt | 삭제 후 content 응답 노출 확인 참고 | Reference |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 최소 범위 구현 | CODE_PRINCIPLES.md | 댓글 삭제 요구사항 해결에 필요한 코드만 수정한다 |
| 명확한 책임 배치 | BACKEND.md | 삭제 정책은 `CommentDeleteService`에서 처리하고 엔티티는 상태 보관만 담당한다 |
| 한국어 주석 유지 | CODE_PRINCIPLES.md | 변경되는 주석은 한국어로 실제 동작을 설명한다 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 해시 계산 위치 | 서비스 레이어 | 표준 라이브러리로 해시 알고리즘을 명시적으로 제어할 수 있다 | `@SQLDelete` SQL 함수 사용 |
| 해시 알고리즘 | SHA-256 | JDK 기본 제공, 추가 의존성 불필요 | MD5, BCrypt |
| 삭제 처리 방식 | 엔티티 필드 직접 갱신 후 저장 | content를 원문 기반으로 계산한 뒤 상태 필드를 함께 기록해야 한다 | `commentRepository.delete` 유지 |

## Data Models (if applicable)

### Comment
| Field | Type | Constraints |
|-------|------|-------------|
| content | TEXT | 삭제 시 SHA-256 hex 문자열로 치환 |
| isDeleted | BOOLEAN | soft delete 여부 |
| deletedAt | TIMESTAMP | 삭제 시각, 미삭제 시 null |

## Implementation Todos

### Todo 1: 계획 파일 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 구현 범위와 검증 기준을 문서화한다.
- **Work**:
  - `.claude/plan/comment/2603/13-hash-comment-delete-content.plan.md` 파일을 생성한다.
  - 관련 파일, 결정 사항, 검증 전략을 현재 요구사항에 맞춰 정리한다.
- **Convention Notes**: 범위와 제외 범위를 한국어로 명확히 적는다.
- **Verification**: 계획 파일의 필수 섹션 존재 여부를 확인한다.
- **Exit Criteria**: 실행 가능한 plan 파일이 저장되어 있다.
- **Status**: completed

### Todo 2: 해시 기반 soft delete 구현
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 댓글 삭제 시 원문을 SHA-256 해시 문자열로 치환한다.
- **Work**:
  - `CommentDeleteService.deleteComment`에서 삭제 대상 댓글의 `content`, `isDeleted`, `deletedAt`을 직접 갱신한다.
  - `MessageDigest` 기반 해시 계산 로직을 추가한다.
  - `Comment` 엔티티의 `@SQLDelete`와 고정 문구 상수를 제거하거나 현재 동작과 일치하도록 정리한다.
- **Convention Notes**: 해시 문자열 생성은 별도 불필요한 유틸 클래스를 만들지 않고 현재 도메인 안에서 최소 범위로 구현한다.
- **Verification**: 코드 리뷰로 삭제 흐름이 `delete` 호출이 아닌 `save` 기반 상태 갱신으로 바뀌었는지 확인한다.
- **Exit Criteria**: 삭제 시 content가 해시 문자열로 저장되는 경로가 코드상 보장된다.
- **Status**: completed

### Todo 3: 검증 및 결과 정리
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 변경 후 컴파일/테스트 영향과 잔여 리스크를 확인한다.
- **Work**:
  - 관련 파일 diff를 검토한다.
  - 가능한 범위에서 Gradle 검증 명령을 실행한다.
  - 결과를 계획 파일과 최종 응답에 반영한다.
- **Convention Notes**: 실패 원인은 실제 로그 기준으로만 정리한다.
- **Verification**: `./gradlew test`
- **Exit Criteria**: 검증 결과와 한계가 정리되어 있다.
- **Status**: completed

## Verification Strategy
삭제 로직 변경 후 `CommentDeleteService`와 `Comment` 엔티티 정의를 대조하고, Gradle 테스트를 실행해 컴파일 및 런타임 문제 여부를 확인한다.
- `./gradlew test`

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-03-13: Plan created
- 2026-03-13: Todo 1 completed — 계획 파일 생성
- 2026-03-13: Todo 2 completed — 서비스 기반 해시 soft delete 구현
- 2026-03-13: Todo 3 completed — 검증 및 결과 정리
- 2026-03-13: Execution complete
