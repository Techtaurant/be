# Fix Post Attachment Orphans

## Business Goal
게시물 수정 시 본문에서 제거된 기존 첨부파일이 계속 남아 저장소와 DB에 잔존하는 문제를 방지한다. 게시물의 최종 본문과 썸네일에 포함되지 않은 attachment는 orphan으로 분류해 정리되도록 만든다.

## Scope
- **In Scope**: `PostWriteService.updatePost()`의 첨부 유지 대상 계산 수정, orphan 삭제 기준 정합성 보장, 관련 단위 테스트 보강
- **Out of Scope**: 첨부파일 서비스 전면 개편, 게시물 생성 로직 변경, API/DTO 스펙 수정

## Codebase Analysis Summary
`PostWriteService`는 게시물 저장 후 본문에 포함된 attachment와 썸네일 attachment를 합쳐 `AttachmentService.confirmAttachmentsByIds()`를 호출하고, 이어서 `deleteOrphanedAttachmentsByIds()`로 유지 대상 외 첨부를 삭제한다. 현재 수정 로직은 기존 `post.thumbnailImage`를 orphan 삭제 keep 목록에 추가해, 본문에서 제거된 기존 attachment가 썸네일로 남아 orphan 정리에서 제외될 수 있다. 테스트는 `PostWriteServiceAttachmentTest`에서 attachment 확정과 orphan 정리 호출 파라미터를 검증하고 있다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostWriteService.kt` | 게시물 생성/수정/삭제와 attachment 후처리 담당 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/attachment/application/AttachmentService.kt` | attachment confirm/orphan delete 서비스 계약 제공 | Reference |
| `src/test/kotlin/com/techtaurant/mainserver/post/application/PostWriteServiceAttachmentTest.kt` | 게시물 attachment 처리 단위 테스트 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 최소 수정 우선 | `.claude/core/CODE_PRINCIPLES.md` | 기존 서비스 흐름을 유지하고 필요한 계산만 작게 조정한다 |
| 테스트 검증 | `.claude/framework/SPRING_BOOT.md` | 테스트는 Given-When-Then 구조와 한글 `@DisplayName`을 유지한다 |
| 완료 검증 | `CLAUDE.md` | 코드 변경 후 `./gradlew spotlessApply && ./gradlew spotlessCheck`를 실행한다 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| orphan 판정 기준 | 수정 후 본문에 포함된 attachment와 최종 썸네일만 유지 | 게시물 최종 상태와 attachment 보존 기준을 일치시킨다 | 기존 thumbnailImage를 무조건 keep |
| 수정 위치 | `PostWriteService.updatePost()` | 본문/썸네일 조합은 게시물 도메인 서비스 책임이다 | `AttachmentService`에 본문 해석 책임 추가 |
| 테스트 방식 | 기존 mock 기반 단위 테스트 보강 | 현재 서비스 계약 검증 패턴을 그대로 활용 가능하다 | 통합 테스트 추가 |

## Implementation Todos

### Todo 1: 계획 파일 생성 및 실행 기준 고정
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 수정 대상과 검증 범위를 계획 파일에 고정한다.
- **Work**:
  - `.claude/plan/post/2604/12-fix-post-attachment-orphans.plan.md` 생성
  - 관련 파일, 수정 범위, verification command를 명시
- **Convention Notes**: 범위를 최소화하고 추정이 필요한 내용은 계획에 명시적으로 적는다.
- **Verification**: 계획 파일 생성 여부와 섹션 완결성 확인
- **Exit Criteria**: plan 파일이 생성되고 모든 필수 섹션이 채워진다.
- **Status**: completed

### Todo 2: 게시물 수정 시 orphan attachment 유지 대상 계산 수정
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 본문에서 제거된 기존 attachment가 orphan 삭제에서 제외되지 않도록 수정한다.
- **Work**:
  - `PostWriteService.updatePost()`에서 최종 썸네일과 유지 attachment 목록 계산 로직 조정
  - `deleteOrphanedAttachmentsByIds()` 호출에 최종 keep 목록만 전달
  - 필요 시 `PostWriteService` private helper를 추가하되 기존 흐름을 유지
- **Convention Notes**: 함수 책임을 크게 늘리지 않고 이름이 의도를 설명하도록 작성한다.
- **Verification**: 관련 단위 테스트 또는 새 테스트로 keep 목록이 기대값과 일치하는지 검증
- **Exit Criteria**: 본문에 없는 기존 attachment가 keep 목록에 포함되지 않는다.
- **Status**: completed

### Todo 3: 게시물 attachment 테스트 기대값 보강
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 수정된 orphan 정리 기준을 테스트로 고정한다.
- **Work**:
  - `PostWriteServiceAttachmentTest`의 기존 케이스 중 변경 영향이 있는 기대값 수정
  - 기존 thumbnail이 본문에 없으면 orphan으로 정리되는 케이스 추가 또는 갱신
- **Convention Notes**: Given-When-Then 구조와 한글 테스트명을 유지한다.
- **Verification**: `PostWriteServiceAttachmentTest` 실행
- **Exit Criteria**: 실패 없이 attachment 관련 시나리오가 모두 통과한다.
- **Status**: completed

### Todo 4: 포맷 및 테스트 검증
- **Priority**: 4
- **Dependencies**: Todo 3
- **Goal**: 변경 사항이 코드 스타일과 테스트 기준을 만족하는지 확인한다.
- **Work**:
  - `./gradlew test --tests com.techtaurant.mainserver.post.application.PostWriteServiceAttachmentTest` 실행
  - `./gradlew spotlessApply && ./gradlew spotlessCheck` 실행
- **Convention Notes**: 저장소 루트에서 명령을 실행하고 실패 시 원인만 최소 수정한다.
- **Verification**: 테스트 및 spotless 명령 성공
- **Exit Criteria**: 지정된 검증 명령이 모두 성공한다.
- **Status**: completed

## Verification Strategy
게시물 수정 attachment 시나리오를 단위 테스트로 검증하고, 마지막에 포맷 검사를 통과시킨다.
- `./gradlew test --tests com.techtaurant.mainserver.post.application.PostWriteServiceAttachmentTest`
- `./gradlew spotlessApply && ./gradlew spotlessCheck`

## Progress Tracking
- Total Todos: 4
- Completed: 4
- Status: Execution complete

## Change Log
- 2026-04-12: Plan created
- 2026-04-12: Todo 1 completed — 계획 파일 생성 및 실행 범위 고정
- 2026-04-12: Todo 2 completed — updatePost가 최종 썸네일 기준으로 orphan keep 목록을 계산하도록 수정
- 2026-04-12: Todo 3 completed — 기존 썸네일이 본문에서 제거되는 시나리오 테스트 추가
- 2026-04-12: Todo 4 completed — attachment 테스트와 spotless 검증 완료
- 2026-04-12: Execution complete
