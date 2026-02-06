# GlobalExceptionHandler 구현

## Business Goal
애플리케이션 전역에서 발생하는 예외를 중앙 집중적으로 처리하여, 일관된 에러 응답을 클라이언트에 반환한다.
Validation 에러, ApiException, 예상치 못한 Exception 각각에 대해 적절한 응답과 로깅을 제공한다.

## Scope
- **In Scope**: GlobalExceptionHandler 생성, ValidationErrorResponse DTO 생성, 3가지 예외 시나리오 핸들링, DefaultStatus에 UNKNOWN_EXCEPTION 추가
- **Out of Scope**: 기존 ApiException/StatusIfs 구조 변경, Security 필터 체인 예외 처리, 테스트 코드 작성

## Codebase Analysis Summary
- `ApiException`은 `RuntimeException`을 상속하며 `StatusIfs` 기반의 상태 코드 시스템 사용
- `ApiResponse<T>`가 공통 응답 DTO로 사용됨 (status, data, message 필드)
- `@Valid`/`@Validated`가 컨트롤러에서 사용 중 → `MethodArgumentNotValidException` 발생 가능
- `DefaultStatus`에 `UNDEFINED_STATUS`(999)가 존재하나, UNKNOWN_EXCEPTION이라는 명시적 상태가 없음

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `common/exception/GlobalExceptionHandler.kt` | 전역 예외 처리 핸들러 | Create |
| `common/dto/ValidationErrorResponse.kt` | Validation 에러 응답 DTO | Create |
| `common/status/DefaultStatus.kt` | 상태 코드 enum | Modify (UNKNOWN_EXCEPTION 추가) |
| `common/exception/ApiException.kt` | 커스텀 예외 | Reference |
| `common/dto/ApiResponse.kt` | 공통 응답 DTO | Reference |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 주석 한국어 | CODE_PRINCIPLES.md | 주석/답변은 한국어 |
| DTO 네이밍 | BACKEND.md | Response로 끝남 |
| KISS | CODE_PRINCIPLES.md | 단순하고 명확한 코드 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 예외 처리 패턴 | @RestControllerAdvice | Spring Boot 표준, 기존 패턴과 일관성 | Custom Filter |
| Validation 응답 | 별도 ValidationErrorResponse DTO | 필드별 에러 목록을 명확히 반환 | ApiResponse에 Map 넣기 |
| ApiException 응답 | 기존 ApiResponse.error() 재사용 | 중복 제거, 일관성 | 새 DTO 생성 |
| 미정의 예외 상태 | DefaultStatus에 UNKNOWN_EXCEPTION 추가 | UNDEFINED_STATUS와 구분되는 명시적 네이밍 | UNDEFINED_STATUS 재사용 |

## Implementation Todos

### Todo 1: DefaultStatus에 UNKNOWN_EXCEPTION 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 예상치 못한 Exception 발생 시 사용할 상태 코드 정의
- **Work**:
  - `DefaultStatus.kt`에 `UNKNOWN_EXCEPTION(500, 998, "Unknown exception occurred")` enum 값 추가
  - `UNDEFINED_STATUS` 앞에 배치
- **Convention Notes**: 기존 enum 스타일 유지
- **Verification**: 빌드 성공
- **Exit Criteria**: UNKNOWN_EXCEPTION enum 값이 정상 정의됨
- **Status**: pending

### Todo 2: ValidationErrorResponse DTO 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Validation 에러 시 필드별 에러 메시지를 담을 응답 DTO 생성
- **Work**:
  - `common/dto/ValidationErrorResponse.kt` 파일 생성
  - `data class ValidationErrorResponse(val errors: Map<String, String>)` 형태
- **Convention Notes**: DTO 네이밍은 ~Response로 끝남, BACKEND.md 준수
- **Verification**: 빌드 성공
- **Exit Criteria**: ValidationErrorResponse DTO가 정상 컴파일됨
- **Status**: pending

### Todo 3: GlobalExceptionHandler 구현
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 2
- **Goal**: 3가지 예외 시나리오를 처리하는 전역 핸들러 구현
- **Work**:
  - `common/exception/GlobalExceptionHandler.kt` 파일 생성
  - `@RestControllerAdvice` 어노테이션 적용
  - `handleValidationException`: MethodArgumentNotValidException, ConstraintViolationException 처리 → 모든 필드 에러 수집 → ApiResponse<ValidationErrorResponse> 반환
  - `handleApiException`: ApiException 처리 → ApiResponse.error(status) 반환
  - `handleException`: Exception 처리 → DefaultStatus.UNKNOWN_EXCEPTION → 로깅 강화 (stack trace + 요청 URI)
  - SLF4J Logger 사용
- **Convention Notes**: KDoc 주석 한국어, KISS 원칙
- **Verification**: 빌드 성공
- **Exit Criteria**: 3가지 핸들러가 정상 작동하며, 로깅이 적절히 설정됨
- **Status**: pending

## Verification Strategy
- `./gradlew :main-server:compileKotlin` 빌드 성공 확인

## Progress Tracking
- Total Todos: 3
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-07: Plan created
