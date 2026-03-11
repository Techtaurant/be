# 댓글 차단 응답 리팩터링과 게시물 차단 조회 쿼리화

## Business Goal
댓글 차단 응답 생성 책임을 읽기 서비스에서 분리해 유지보수성을 높이고, 게시물 차단 정책을 조회 쿼리에서 직접 처리해 서비스 레이어의 중간 상태 의존을 줄인다.

## Scope
- **In Scope**: 댓글 차단 응답 assembler 추가, `CommentListResponse` 생성 방식 단순화, 게시물 목록/상세 차단 조건을 repository query로 이동, 관련 테스트 갱신
- **Out of Scope**: 차단 API 스펙 변경, 차단 정책 변경, 댓글 조회 전체를 projection 기반으로 재작성

## Codebase Analysis Summary
댓글 응답은 현재 `CommentReadService`가 좋아요 상태와 차단 마스킹을 모두 조합해 `CommentListResponse.from(...)`에 과도한 optional argument를 넘기는 구조다. 게시물 조회는 `PostListReadService`와 `PostDetailReadService`가 `UserBanService.getBannedUserIds()`를 호출해 application 레이어에서 차단 상태를 미리 계산하고 repository로 전달한다. 기존 테스트는 댓글 통합 테스트와 게시물 서비스/리포지토리/컨트롤러 테스트로 나뉘어 있어 리팩터링 이후에도 같은 레이어 경계에서 검증하는 것이 적절하다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentReadService.kt` | 댓글 조회 orchestration | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/dto/CommentListResponse.kt` | 댓글 응답 DTO | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/BannedUserMaskingService.kt` | 차단 사용자 마스킹 규칙 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentResponseAssembler.kt` | 댓글 응답 조립 전용 클래스 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostListReadService.kt` | 게시물 목록 조회 orchestration | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostDetailReadService.kt` | 게시물 상세 조회 orchestration | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepositoryCustom.kt` | 게시물 동적 조회 인터페이스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepositoryCustomImpl.kt` | 게시물 동적 조회 구현 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepository.kt` | 게시물 상세 조회 확장 포인트 | Reference |
| `src/test/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentReadControllerTest.kt` | 댓글 차단 응답 통합 테스트 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/post/application/PostListReadServiceTest.kt` | 게시물 목록 서비스 테스트 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepositoryCustomImplTest.kt` | 게시물 동적 조회 repository 테스트 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadOpenApiControllerIntegrationTest.kt` | 게시물 목록/상세 차단 통합 테스트 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 완료 체크리스트 | `CLAUDE.md` | 코드 변경 후 `./gradlew spotlessApply && ./gradlew spotlessCheck`를 반드시 통과 |
| 책임 분리 | 기존 `application` / `infrastructure.out` 구조 | 서비스는 orchestration 위주, 조회 조건은 repository query에서 처리 |
| DTO 단순성 | 기존 DTO 패턴 | DTO는 optional parameter 남용보다 명시적 팩토리/조립 클래스를 우선 |
| 테스트 범위 | 기존 테스트 파일 구성 | 서비스, repository, controller 레이어 각각 기존 위치에서 회귀 검증 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 댓글 차단 응답 위치 | `CommentResponseAssembler` 신설 | 좋아요 상태와 차단 마스킹을 한 곳에서 조립 | `CommentReadService` 내부 유지 |
| 댓글 DTO 생성 방식 | 일반 응답/차단 응답을 분리한 명시적 팩토리 | optional 인자 남용 제거 | 단일 `from(...)` 유지 |
| 게시물 목록 차단 처리 | repository query에서 `NOT EXISTS user_bans` 조건 적용 | 서비스 선조회 제거, 페이징 조건과 결합 일관성 확보 | `excludedAuthorIds` 유지 |
| 게시물 상세 차단 처리 | repository 기반 차단 여부 조회 추가 | 상세도 동일한 조회 정책 경로 유지 | `UserBanService.getBannedUserIds()` 유지 |

## Implementation Todos

### Todo 1: 댓글 차단 응답 조립 책임 분리
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 댓글 조회 서비스에서 차단 마스킹 세부 구현을 제거한다.
- **Work**:
  - `CommentResponseAssembler`를 추가해 댓글 목록 + 좋아요 상태 + 현재 사용자 ID를 받아 `CommentListResponse` 목록으로 변환
  - `CommentListResponse`에 일반 응답과 차단 응답용 명시적 생성 경로를 만든다
  - `CommentReadService`는 repository 호출과 pagination orchestration만 담당하도록 정리한다
- **Convention Notes**: DTO에 과도한 책임을 넣지 말고, assembler가 마스킹 규칙과 응답 조립을 캡슐화한다.
- **Verification**: `./gradlew test -x jacocoTestCoverageVerification --tests '*CommentReadControllerTest'`
- **Exit Criteria**: `CommentReadService`에서 차단 응답 필드 조합 코드가 제거되고 댓글 통합 테스트가 통과한다.
- **Status**: completed

### Todo 2: 게시물 목록/상세 차단 조건을 query로 이동
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 게시물 조회 시 차단 관계를 서비스 선조회 없이 repository query에서 처리한다.
- **Work**:
  - `PostRepositoryCustom.findPostsWithConditions`에서 `excludedAuthorIds` 인자를 제거하고 viewer 기준 차단 조건을 받을 수 있게 시그니처를 정리한다
  - `PostRepositoryCustomImpl`에 `user_bans`를 기준으로 한 `NOT EXISTS` 또는 동등한 subquery 조건을 추가한다
  - `PostListReadService`에서 `UserBanService.getBannedUserIds()` 호출을 제거한다
  - `PostDetailReadService`는 repository 기반 차단 여부 조회 또는 차단 조건 포함 조회로 정리한다
- **Convention Notes**: 조회 정책은 persistence layer에 두고, application service는 입력 조합과 후처리만 담당한다.
- **Verification**: `./gradlew test -x jacocoTestCoverageVerification --tests '*PostListReadServiceTest' --tests '*PostRepositoryCustomImplTest' --tests '*PostReadOpenApiControllerIntegrationTest'`
- **Exit Criteria**: 게시물 목록/상세에서 차단 사용자 비노출이 유지되고 서비스 레이어의 ban ID 선조회가 제거된다.
- **Status**: completed

### Todo 3: 회귀 검증과 포맷 정리
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 리팩터링 후 기존 기능 회귀와 포맷 규칙을 확인한다.
- **Work**:
  - 변경된 댓글/게시물 관련 테스트를 묶어서 실행한다
  - `spotlessApply`와 `spotlessCheck`를 실행한다
  - 필요 시 테스트와 포맷 결과에 맞춰 import/order를 정리한다
- **Convention Notes**: 변경 영향 범위에 집중해 빠르게 검증하되, `CLAUDE.md` 완료 체크리스트는 반드시 지킨다.
- **Verification**:
  - `./gradlew test -x jacocoTestCoverageVerification --tests '*CommentReadControllerTest' --tests '*PostListReadServiceTest' --tests '*PostRepositoryCustomImplTest' --tests '*PostReadOpenApiControllerIntegrationTest'`
  - `./gradlew spotlessApply && ./gradlew spotlessCheck`
- **Exit Criteria**: 대상 테스트와 spotless가 모두 통과한다.
- **Status**: completed

## Verification Strategy
댓글과 게시물 조회 레이어를 각각 통합/단위 테스트로 검증하고, 마지막에 spotless 규칙까지 통과시킨다.
- `./gradlew test -x jacocoTestCoverageVerification --tests '*CommentReadControllerTest' --tests '*PostListReadServiceTest' --tests '*PostRepositoryCustomImplTest' --tests '*PostReadOpenApiControllerIntegrationTest'`
- `./gradlew spotlessApply && ./gradlew spotlessCheck`

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-03-11: Plan created
- 2026-03-11: Todo 1 completed — 댓글 차단 응답 조립을 assembler로 분리하고 DTO 팩토리를 단순화
- 2026-03-11: Todo 2 completed — 게시물 차단 정책을 repository query 기반으로 이동하고 서비스 ban ID 선조회를 제거
- 2026-03-11: Todo 3 completed — 댓글/게시물 회귀 테스트와 spotless 검증을 완료
