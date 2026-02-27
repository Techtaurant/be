# 게시물 읽음 상태 API 리팩토링

## Business Goal
게시물 읽음 상태 변경 API의 경로를 RESTful 컨벤션에 맞게 `read-logs`로 변경하고,
Swagger 에러 응답 명세를 구체화하여 프론트엔드 개발자가 에러 핸들링을 정확히 할 수 있도록 한다.

## Scope
- **In Scope**:
  - API 경로 `/api/posts/{postId}/read` → `/api/posts/{postId}/read-logs` 변경
  - `UserStatus`에 `USER_NOT_FOUND(404, 1002)` 추가
  - `PostReadLogService`에서 `USER_NOT_FOUND` 사용
  - Swagger `@ApiResponses`에 구체적 에러 코드/메시지 명시
  - 테스트 코드 경로 및 에러 코드 업데이트
- **Out of Scope**:
  - Docs 인터페이스 분리
  - 기존 `UserStatus.ID_NOT_FOUND` 변경
  - 다른 컨트롤러의 Swagger 개선

## Codebase Analysis Summary

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `main-server/.../post/infrastructure/in/PostReadController.kt` | 읽음 상태 변경 컨트롤러 | Modify |
| `main-server/.../post/application/PostReadLogService.kt` | 읽음 상태 토글 서비스 | Modify |
| `main-server/.../user/enums/UserStatus.kt` | 사용자 에러 상태 enum | Modify |
| `main-server/.../post/infrastructure/in/PostReadControllerTest.kt` | E2E 테스트 | Modify |
| `main-server/.../post/application/PostReadLogServiceTest.kt` | 서비스 통합 테스트 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Enum 구조 | `PostStatus`, `UserStatus` | `(httpStatusCode, customStatusCode, description)` + `StatusIfs` 구현 |
| Swagger 스타일 | 기존 컨트롤러들 | `@ApiResponses` + `io.swagger.v3.oas.annotations.responses.ApiResponse` |
| 에러 메시지 | BACKEND.md | 한국어 description |
| 테스트 패턴 | SPRING_BOOT.md | Given-When-Then, `@DisplayName` 한국어 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 사용자 미존재 에러 | `USER_NOT_FOUND(404, 1002)` 신규 추가 | 기존 `ID_NOT_FOUND(500)`과 분리, RESTful 404 | 기존 변경 (다른 참조 영향) |
| Swagger 상세화 | description에 에러코드/메시지 텍스트 포함 | 프로젝트 기존 패턴 유지 | content+Schema 방식 |

## API Contracts

### POST /api/posts/{postId}/read-logs
- Headers: `Authorization: Bearer {token}`
- Request: `{ "isRead": boolean }`
- Response 200: `{ "status": 200, "data": null, "message": "성공" }`
- Response 401: Spring Security 처리
- Response 404 (게시물): `{ "status": 3001, "data": null, "message": "게시물을 찾을 수 없습니다" }`
- Response 404 (사용자): `{ "status": 1002, "data": null, "message": "사용자를 찾을 수 없습니다" }`

## Implementation Todos

### Todo 1: UserStatus에 USER_NOT_FOUND 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: HTTP 404에 매핑되는 사용자 미존재 에러 코드 추가
- **Work**:
  - `UserStatus.kt`에 `USER_NOT_FOUND(404, 1002, "사용자를 찾을 수 없습니다")` enum 값 추가
- **Convention Notes**: 기존 enum 구조 `(httpStatusCode, customStatusCode, description)` 준수
- **Verification**: 빌드 성공
- **Exit Criteria**: `UserStatus.USER_NOT_FOUND` 접근 가능
- **Status**: pending

### Todo 2: PostReadLogService에서 USER_NOT_FOUND 사용
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 사용자 미존재 시 404 에러 반환
- **Work**:
  - `PostReadLogService.toggleReadStatus`에서 `UserStatus.ID_NOT_FOUND` → `UserStatus.USER_NOT_FOUND`로 변경
- **Convention Notes**: 기존 코드 패턴 유지
- **Verification**: 빌드 성공
- **Exit Criteria**: 사용자 미존재 시 HTTP 404 + custom code 1002 반환
- **Status**: pending

### Todo 3: API 경로 변경 및 Swagger 상세화
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: API 경로를 read-logs로 변경하고 Swagger 에러 응답 명세를 구체화
- **Work**:
  - `PostReadController.kt`의 `@PostMapping("/{postId}/read")` → `@PostMapping("/{postId}/read-logs")`
  - `@ApiResponses`의 각 에러 응답 description에 구체적 에러 코드/메시지 추가:
    - 200: "읽음 상태 변경 성공"
    - 401: "인증되지 않은 사용자"
    - 404: "게시물을 찾을 수 없음 (status: 3001, message: 게시물을 찾을 수 없습니다) / 사용자를 찾을 수 없음 (status: 1002, message: 사용자를 찾을 수 없습니다)"
- **Convention Notes**: 기존 `@ApiResponses` 스타일 유지
- **Verification**: 빌드 성공
- **Exit Criteria**: Swagger UI에서 변경된 경로와 상세 에러 명세 확인 가능
- **Status**: pending

### Todo 4: 테스트 코드 업데이트
- **Priority**: 3
- **Dependencies**: Todo 2, Todo 3
- **Goal**: 변경된 API 경로와 에러 코드에 맞게 테스트 수정
- **Work**:
  - `PostReadControllerTest.kt`: 모든 API 호출 경로 `/api/posts/{postId}/read` → `/api/posts/{postId}/read-logs`
  - `PostReadLogServiceTest.kt`: `toggleReadStatus_withNonExistentUser_shouldThrowException`에서 `UserStatus.ID_NOT_FOUND` → `UserStatus.USER_NOT_FOUND`
- **Convention Notes**: Given-When-Then 패턴, `@DisplayName` 한국어
- **Verification**: 테스트 전체 통과
- **Exit Criteria**: 모든 관련 테스트 PASS
- **Status**: pending

### Todo 5: spotlessApply 및 최종 검증
- **Priority**: 4
- **Dependencies**: Todo 4
- **Goal**: 코드 포맷팅 및 빌드 최종 확인
- **Work**:
  - `cd main-server && ./gradlew spotlessApply && ./gradlew spotlessCheck`
- **Convention Notes**: CLAUDE.md Completion Checklist
- **Verification**: spotlessCheck 통과
- **Exit Criteria**: spotless 통과 + 빌드 성공
- **Status**: pending

## Verification Strategy
- `./gradlew spotlessApply && ./gradlew spotlessCheck`
- 관련 테스트 실행

## Progress Tracking
- Total Todos: 5
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-27: Plan created
