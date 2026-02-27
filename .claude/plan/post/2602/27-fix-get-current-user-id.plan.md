# getCurrentUserId() 버그 수정

## Business Goal
`PostListReadService.getCurrentUserId()`가 JWT principal(UUID)을 `User`로 캐스팅하여 항상 null을 반환하는 버그를 수정한다. 이를 통해 `getPosts()`의 `visibleToUserId`와 `getPostsByUserId()`의 본인/타인 분기가 정상 동작하도록 한다.

## Scope
- **In Scope**: `getCurrentUserId()` 캐스팅 수정, 테스트 mock 업데이트
- **Out of Scope**: `getPostDetail()` (이미 `@AuthenticationPrincipal userId: UUID?`로 정상 동작)

## Codebase Analysis Summary
- `JwtAuthenticationFilter`가 `claims.userId`(UUID)를 principal로 저장
- `@AuthenticationPrincipal userId: UUID?` 패턴이 프로젝트 전체에서 사용됨
- `getCurrentUserId()`만 `(principal as? User)?.id` 패턴을 사용하여 불일치

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `post/application/PostListReadService.kt` | `getCurrentUserId()` 메서드 | Modify |
| `post/application/PostListReadServiceTest.kt` | 테스트 mock 수정 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Principal 타입 | JwtAuthenticationFilter | principal은 UUID (User가 아님) |
| Given-When-Then | SPRING_BOOT.md | 모든 테스트는 GWT 패턴 |
| spotless | CLAUDE.md | `spotlessApply && spotlessCheck` 필수 |

## Implementation Todos

### Todo 1: getCurrentUserId() 캐스팅 수정
- **Priority**: 1
- **Dependencies**: none
- **Goal**: principal을 UUID로 직접 캐스팅하여 로그인 사용자 ID를 정상 반환
- **Work**:
  - `PostListReadService.kt`의 `getCurrentUserId()` 메서드에서 `(authentication?.principal as? User)?.id`를 `authentication?.principal as? UUID`로 변경
  - User import 제거 (더 이상 사용하지 않는 경우)
- **Convention Notes**: JwtAuthenticationFilter가 UUID를 principal로 저장하는 패턴에 맞춤
- **Verification**: 컴파일 성공
- **Exit Criteria**: `getCurrentUserId()`가 UUID principal을 정상 반환
- **Status**: completed

### Todo 2: 테스트 mock 업데이트
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 테스트의 `setCurrentUser()` 헬퍼가 UUID를 principal로 설정하도록 수정
- **Work**:
  - `PostListReadServiceTest.kt`의 `setCurrentUser()` 메서드에서 `authentication.principal`이 `user`가 아닌 `user.id`를 반환하도록 변경
  - User import는 테스트 데이터 생성에 여전히 필요하므로 유지
- **Convention Notes**: Given-When-Then, @DisplayName 한국어
- **Verification**: 모든 테스트 통과
- **Exit Criteria**: 기존 9개 단위 테스트 모두 통과
- **Status**: completed

## Verification Strategy
- `./gradlew spotlessApply && ./gradlew spotlessCheck`
- `./gradlew test --tests PostListReadServiceTest -x jacocoTestCoverageVerification`

## Progress Tracking
- Total Todos: 2
- Completed: 2
- Status: Execution complete

## Change Log
- 2026-02-27: Plan created
- 2026-02-27: All todos completed. 9 tests passed, spotless check passed
