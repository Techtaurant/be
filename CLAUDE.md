## Completion Checklist

코드 변경 작업 완료 시 반드시 아래를 실행하고 통과해야 한다.

```bash
cd main-server && ./gradlew spotlessApply && ./gradlew spotlessCheck
```

## Swagger 에러 응답 명세 규칙

Swagger `@ApiResponse`에 에러를 문서화할 때, `GlobalExceptionHandler`가 실제로 반환하는 응답 포맷과 일치하도록 `@Content` + `@Schema` + `@ExampleObject`를 반드시 포함한다.

### 에러 응답 유형

**1. Validation 에러 (400)** — `@Valid`, `@Min`, `@Max` 등 검증 실패 시

`GlobalExceptionHandler`가 `ApiResponse<ValidationErrorResponse>`를 반환한다.

```json
{"status": 400, "data": {"errors": {"필드명": "에러메시지"}}, "message": "Wrong Request"}
```

**2. 비즈니스 에러 (ApiException)** — 도메인 규칙 위반 시

`GlobalExceptionHandler`가 `ApiResponse<null>`을 반환한다. `status`는 도메인별 `StatusIfs` enum의 `customStatusCode`를 사용한다.

```json
{"status": 3001, "data": null, "message": "게시물을 찾을 수 없습니다"}
```

### 작성 패턴

Swagger의 `ApiResponse`와 프로젝트의 `ApiResponse`가 이름 충돌하므로, Swagger 쪽은 `import ... as SwaggerApiResponse` alias를 사용한다.

```kotlin
import com.techtaurant.mainserver.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

// Validation 에러
SwaggerApiResponse(
    responseCode = "400",
    description = "에러 상황 설명",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = ApiResponse::class),
            examples = [
                ExampleObject(
                    name = "예시 이름",
                    value = EXAMPLE_CONSTANT, // companion object const val 사용
                ),
            ],
        ),
    ],
)

// 비즈니스 에러
SwaggerApiResponse(
    responseCode = "404",
    description = "에러 상황 설명",
    content = [
        Content(
            mediaType = "application/json",
            schema = Schema(implementation = ApiResponse::class),
            examples = [
                ExampleObject(
                    name = "예시 이름",
                    value = EXAMPLE_CONSTANT,
                ),
            ],
        ),
    ],
)
```

### 주의사항

- ExampleObject의 `value`는 줄 길이(140자) 제한을 지키기 위해 `companion object`의 `const val`로 분리한다
- `status` 필드 값은 반드시 해당 도메인의 `StatusIfs` enum에 정의된 `customStatusCode`를 사용한다 (DefaultStatus: 400, PostStatus: 3001~3009 등)
- `message` 필드 값은 해당 `StatusIfs` enum의 `description`과 일치시킨다

## Dev Test User API

dev 환경에서 OAuth2 플로우 없이 테스트 사용자로 JWT를 발급받을 수 있는 API.

### 동작 원리

- `@Profile("dev")` 어노테이션으로 dev 프로파일에서만 빈이 등록됨
- dev가 아닌 환경에서는 컨트롤러/서비스 자체가 존재하지 않아 404 반환
- 테스트 사용자는 `OAuthProvider.DEV_LOCAL` provider로 구분

### 사용법

```bash
curl -X POST http://localhost:8080/open-api/dev/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier": "dev-test-user", "password": "dev-password"}' \
  -c cookies.txt
```

- `identifier`: 원하는 테스트 사용자명 (없으면 자동 생성, 있으면 기존 사용자 사용)
- `password`: 고정값 `dev-password`
- 응답 쿠키에 `accessToken`, `refreshToken`이 설정됨

### dev 프로파일 활성화 방법

**환경변수 (권장)**:
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

**IntelliJ Run Configuration**:
1. Run/Debug Configurations 열기
2. Active profiles에 `dev` 입력

**application.yml (비권장, 커밋 주의)**:
```yaml
spring:
  profiles:
    active: dev
```

### 보안 주의사항

- `SPRING_PROFILES_ACTIVE=dev`는 **로컬 개발 환경에서만** 사용
- Production/Staging 환경에서는 절대로 `dev` 프로파일을 활성화하지 않아야 함
- CI/CD 파이프라인에서 dev 프로파일이 포함되지 않도록 확인
