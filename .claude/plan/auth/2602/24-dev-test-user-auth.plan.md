# Dev Test User Auth API

## Business Goal
dev 환경에서 OAuth2 없이 테스트 사용자로 빠르게 인증하여 JWT를 발급받을 수 있는 API를 제공한다.
프론트엔드 개발자와 백엔드 개발자가 로컬 개발 시 Google OAuth 플로우 없이 즉시 인증된 상태로 API를 테스트할 수 있게 한다.
Production 환경에서는 절대로 노출되지 않아야 한다.

## Scope
- **In Scope**: dev 전용 로그인 API, 테스트 사용자 자동 생성, JWT 쿠키 발급, SecurityConfig dev 설정, OAuthProvider DEV_LOCAL 추가, CLAUDE.md 가이드
- **Out of Scope**: password 해싱, Flyway 마이그레이션, 테스트 코드, dev 프로파일용 application-dev.yml 생성 (이미 env로 관리)

## Codebase Analysis Summary
- **인증 방식**: OAuth2 (Google) → `OAuth2SuccessHandler`에서 JWT 발급 + 쿠키 설정
- **JWT**: `JwtTokenProvider.createAccessToken(userId, role)`, `createRefreshToken(userId)` → 쿠키(`accessToken`, `refreshToken`)로 전달
- **User Entity**: `name`, `email`, `provider(OAuthProvider)`, `identifier`, `role(UserRole)`, `profileImageUrl` + `EntityBase(id, createdAt, updatedAt)`
- **User 식별**: `identifier + provider` unique constraint
- **API 구조**: `/open-api/**` (인증 불필요), `/api/**` (인증 필요), Swagger annotations 사용
- **응답 래퍼**: `ApiResponse<T>` (status, data, message)
- **에러 처리**: `ApiException(StatusIfs)` 패턴
- **Security**: `SecurityConfig` → `JwtAuthenticationFilter` + OAuth2 설정
- **토큰 캐시**: `TokenCachePort` (Caffeine) — refresh token 저장

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `security/enums/OAuthProvider.kt` | OAuth provider enum | Modify — `DEV_LOCAL` 추가 |
| `security/config/SecurityConfig.kt` | Security filter chain | Modify — dev-api permitAll 추가 |
| `security/infrastructure/in/DevTestAuthController.kt` | dev 전용 컨트롤러 | Create |
| `security/service/DevTestAuthService.kt` | dev 전용 인증 서비스 | Create |
| `security/dto/DevTestLoginRequest.kt` | 로그인 요청 DTO | Create |
| `security/jwt/JwtTokenProvider.kt` | JWT 토큰 생성 | Reference |
| `security/helper/CookieHelper.kt` | 쿠키 설정 | Reference |
| `security/cache/TokenCachePort.kt` | 토큰 캐시 | Reference |
| `user/infrastructure/out/UserRepository.kt` | 사용자 조회 | Reference |
| `user/entity/User.kt` | 사용자 엔티티 | Reference |
| `CLAUDE.md` | 프로젝트 가이드 | Modify — dev 환경 가이드 추가 |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO 네이밍 | BACKEND.md | `~Request`, `~Response` 접미사 |
| API description | BACKEND.md | Swagger 설명 한국어 |
| 응답 래퍼 | ApiResponse.kt | `ApiResponse<T>` 사용 |
| Enum 파일 분리 | BACKEND.md | Inner class 금지, 각각 파일로 분리 |
| UUID v7 | EntityBase.kt | ID는 `@UuidV7` 사용 |
| 코드 포맷 | CLAUDE.md | `spotlessApply` + `spotlessCheck` |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 프로파일 제어 | `@Profile("dev")` | Spring 표준, 빈 미등록으로 완전 차단 | `@ConditionalOnProperty` |
| API prefix | `/dev-api/auth/login` | 기존 `/open-api`, `/api`와 구분 | `/open-api/dev/auth/login` |
| Password 검증 | 고정 password `dev-password` | dev 전용, 보안 불필요 | BCrypt |
| Provider 구분 | `OAuthProvider.DEV_LOCAL` | 기존 OAuth 사용자와 구분 | 별도 필드 |
| Token 발급 | 쿠키 기반 (OAuth2SuccessHandler와 동일) | 프론트엔드 일관성 | Response body |
| SecurityConfig | 기존 SecurityConfig에 `/dev-api/**` permitAll 추가 | 단순, dev 빈이 없으면 경로 자체가 404 | 별도 SecurityConfig 빈 분리 |

## API Contracts

### POST /dev-api/auth/login
- Headers: `Content-Type: application/json`
- Request:
```json
{
  "identifier": "dev-test-user",
  "password": "dev-password"
}
```
- Response (200 OK):
```json
{
  "status": 200,
  "data": null,
  "message": "OK"
}
```
- Cookies Set:
  - `accessToken`: JWT access token
  - `refreshToken`: JWT refresh token
- Note: `password`가 `dev-password`와 일치하지 않으면 401 에러. dev 프로파일에서만 동작.

## Implementation Todos

### Todo 1: OAuthProvider에 DEV_LOCAL 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 테스트 사용자를 기존 OAuth 사용자와 구분하기 위한 provider 값 추가
- **Work**:
  - `security/enums/OAuthProvider.kt`에 `DEV_LOCAL("dev-local")` 값 추가
- **Convention Notes**: 기존 enum 패턴 유지
- **Verification**: `./gradlew spotlessApply && ./gradlew spotlessCheck`
- **Exit Criteria**: 빌드 성공, `OAuthProvider.DEV_LOCAL` 사용 가능
- **Status**: pending

### Todo 2: DevTestLoginRequest DTO 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: dev 로그인 요청 DTO 정의
- **Work**:
  - `security/dto/DevTestLoginRequest.kt` 파일 생성
  - 필드: `identifier: String`, `password: String`
  - `@Schema` annotation으로 Swagger 설명 추가
  - `@field:NotBlank` validation 추가
- **Convention Notes**: BACKEND.md — Request 접미사, Description 필수
- **Verification**: 빌드 성공
- **Exit Criteria**: DTO 클래스 컴파일 성공
- **Status**: pending

### Todo 3: DevTestAuthService 생성
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 2
- **Goal**: 테스트 사용자 조회/생성 및 JWT 발급 로직 구현
- **Work**:
  - `security/service/DevTestAuthService.kt` 파일 생성
  - `@Service`, `@Profile("dev")` annotation
  - 의존성: `UserRepository`, `JwtTokenProvider`, `JwtProperties`, `CookieHelper`, `TokenCachePort`
  - `execute(request: DevTestLoginRequest, response: HttpServletResponse)` 메서드:
    1. password 검증 (`dev-password` 고정값, 불일치 시 `ApiException` throw)
    2. `userRepository.findByIdentifierAndProvider(request.identifier, OAuthProvider.DEV_LOCAL)` 조회
    3. 없으면 `User` 생성 (name=identifier, email="{identifier}@dev.local", provider=DEV_LOCAL, identifier=request.identifier, role=USER, profileImageUrl="")
    4. `jwtTokenProvider.createAccessToken(userId, role)` + `createRefreshToken(userId)`
    5. `tokenCacheManager.saveRefreshToken(userId, refreshToken)`
    6. `cookieHelper.addCookie(response, accessToken/refreshToken, ...)`
- **Convention Notes**: 기존 `OAuth2SuccessHandler`의 토큰 발급 패턴 그대로 따라감
- **Verification**: 빌드 성공
- **Exit Criteria**: Service 클래스 컴파일 성공, 기존 코드와 동일한 JWT+쿠키 발급 플로우
- **Status**: pending

### Todo 4: SecurityConfig에 /dev-api/** permitAll 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: dev-api 경로 인증 없이 접근 허용
- **Work**:
  - `SecurityConfig.securityFilterChain`의 `authorizeHttpRequests` 블록에 `.requestMatchers("/dev-api/**").permitAll()` 추가
  - 기존 permitAll 목록(swagger, oauth2 등) 바로 아래에 추가
- **Convention Notes**: 기존 SecurityConfig 패턴 유지
- **Verification**: 빌드 성공
- **Exit Criteria**: `/dev-api/**` 경로가 인증 없이 접근 가능
- **Status**: pending

### Todo 5: DevTestAuthController 생성
- **Priority**: 3
- **Dependencies**: Todo 2, Todo 3, Todo 4
- **Goal**: dev 전용 로그인 API 엔드포인트 구현
- **Work**:
  - `security/infrastructure/in/DevTestAuthController.kt` 파일 생성
  - `@RestController`, `@Profile("dev")`, `@RequestMapping("/dev-api/auth")`, `@Tag(name = "Dev Auth", description = "개발 환경 전용 인증 API")`
  - `POST /login` 엔드포인트:
    - `@Operation(summary = "개발용 테스트 로그인")` Swagger annotation
    - `@ApiResponses` — 200 성공, 401 비밀번호 불일치
    - `@RequestBody @Valid DevTestLoginRequest` + `HttpServletResponse`
    - `devTestAuthService.execute(request, response)` 호출
    - `ApiResponse.ok()` 반환
- **Convention Notes**: 기존 `AuthOpenApiController` 패턴 참고, Swagger 한국어 설명
- **Verification**: `./gradlew spotlessApply && ./gradlew spotlessCheck`
- **Exit Criteria**: `/dev-api/auth/login` 엔드포인트가 Swagger에 노출되고, dev 프로파일에서만 빈 등록
- **Status**: pending

### Todo 6: CLAUDE.md에 dev 환경 가이드 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: dev 환경에서 테스트 사용자 API 사용 방법과 인프라 설정 가이드 작성
- **Work**:
  - `CLAUDE.md`에 `## Dev Test User API` 섹션 추가
  - 내용:
    - API 사용법 (curl 예시)
    - `SPRING_PROFILES_ACTIVE=dev` 설정 방법 (환경변수, application.yml, IntelliJ)
    - dev 프로파일에서만 동작하는 원리 설명 (`@Profile("dev")`)
    - 보안 주의사항 (production에서 dev 프로파일 사용 금지)
- **Convention Notes**: 기존 CLAUDE.md 형식 유지
- **Verification**: 문서 내용 확인
- **Exit Criteria**: 다른 개발자가 가이드만 보고 dev 환경 설정 및 API 사용 가능
- **Status**: pending

### Todo 7: 최종 빌드 검증
- **Priority**: 4
- **Dependencies**: Todo 1, 2, 3, 4, 5, 6
- **Goal**: 전체 코드 빌드 및 린트 통과 확인
- **Work**:
  - `cd main-server && ./gradlew spotlessApply && ./gradlew spotlessCheck`
  - `./gradlew build` (전체 빌드)
- **Convention Notes**: CLAUDE.md completion checklist 준수
- **Verification**: 빌드 성공, spotless 통과
- **Exit Criteria**: BUILD SUCCESSFUL
- **Status**: pending

## Verification Strategy
- `./gradlew spotlessApply && ./gradlew spotlessCheck` — 코드 포맷 검증
- `./gradlew build` — 전체 빌드 성공
- dev 프로파일이 아닌 경우 `DevTestAuthController`, `DevTestAuthService` 빈이 등록되지 않는지 확인 (`@Profile("dev")`)

## Progress Tracking
- Total Todos: 7
- Completed: 7
- Status: Execution complete

## Change Log
- 2026-02-24: Plan created
- 2026-02-24: All todos completed, build successful
