# 게시물 목록 조회 Optional Auth 읽음 상태 수정

## Business Goal
게시물 목록 조회 API에서 로그인한 사용자의 읽음 여부(isRead)가 항상 false로 반환되는 버그를 수정하여,
Optional Auth 기반으로 정확한 읽음 상태를 표시한다.

## Scope
- **In Scope**:
  - `PostReadOpenApiController.getPosts()`에 `@AuthenticationPrincipal userId: UUID?` 추가
  - `PostListReadService.getPosts()`에 `userId: UUID?` 파라미터 추가
  - `getCurrentUserId()` 메서드 제거
  - 테스트 코드 업데이트
  - spotlessApply
- **Out of Scope**:
  - PostDetailReadService 변경 (이미 올바르게 구현됨)

## Relevant Files
| File | Role | Action |
|------|------|--------|
| `main-server/.../post/application/PostListReadService.kt` | 게시물 목록 조회 서비스 | Modify |
| `main-server/.../post/infrastructure/in/PostReadOpenApiController.kt` | Open API 컨트롤러 | Modify |

## Implementation Todos

### Todo 1: PostListReadService.getPosts()에 userId 파라미터 추가 및 getCurrentUserId 제거
- **Priority**: 1
- **Dependencies**: none
- **Goal**: SecurityContext 의존 제거, 명시적 파라미터 전달 방식으로 전환
- **Status**: pending

### Todo 2: PostReadOpenApiController.getPosts()에 @AuthenticationPrincipal 추가
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 컨트롤러에서 Optional Auth userId를 서비스로 전달
- **Status**: pending

### Todo 3: spotlessApply 및 빌드 검증
- **Priority**: 3
- **Dependencies**: Todo 2
- **Status**: pending

## Progress Tracking
- Total Todos: 3
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-27: Plan created
