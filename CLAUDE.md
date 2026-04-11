## Completion Checklist

코드 변경 작업 완료 시 반드시 아래를 실행하고 통과해야 한다.

```bash
./gradlew spotlessApply && ./gradlew spotlessCheck
```

## Terraform 변수 파일 규칙

`techtaurant-infra/dev`, `techtaurant-infra/monitoring` 작업 시 변수 예제와 실제 입력값은 repo root 기준으로 관리한다.

- 커밋 가능한 예제값은 root `terraform.tfvars.example`에 둔다.
- 실제 민감값은 root `terraform.tfvars`에만 두고 커밋하지 않는다.
- 각 스택은 root tfvars를 자동 인식하지 않으므로 반드시 `-var-file=../../terraform.tfvars`를 명시한다.

```bash
terraform -chdir=techtaurant-infra/dev plan -var-file=../../terraform.tfvars
terraform -chdir=techtaurant-infra/monitoring plan -var-file=../../terraform.tfvars
```

## Swagger 명세 규칙

Swagger 명세는 Controller 구현체에 직접 흩뿌리지 않고, 반드시 별도 `*ControllerDocs` interface에 작성한다. 구현체 Controller는 라우팅과 서비스 호출만 담당하고, Swagger/OpenAPI 어노테이션은 interface로 분리한다.

### 구조 규칙

- Controller 구현체에는 Swagger 어노테이션을 직접 작성하지 않는다.
- Swagger 전용 interface 이름은 반드시 `{Controller명}Docs`로 맞춘다.
- Controller는 해당 Docs interface를 `implements` 하도록 유지한다.
- Swagger의 `ApiResponse`와 프로젝트의 `ApiResponse`가 이름 충돌하므로, Swagger 쪽은 반드시 `import ... as SwaggerApiResponse` alias를 사용한다.
- OpenAPI에 노출되는 RequestParam, PathVariable, AuthenticationPrincipal 시그니처는 Controller와 Docs interface가 정확히 같아야 한다.
- 특히 `@Valid`, `@NotBlank`, `@Min`, `@Max` 같은 validation 어노테이션도 Controller와 Docs interface에 동일하게 선언한다. 하나라도 어긋나면 런타임에 `ConstraintDeclarationException`이 발생할 수 있다.

### 성공 응답 규칙

- 성공 응답은 `responseCode`와 `description`만 명시한다. `content`는 선언하지 않는다.
- `content`에 `Schema(implementation = ApiResponse::class)`를 명시하면 SpringDoc이 제네릭 타입 정보를 잃어버려 `data: any`로 표시된다. SpringDoc이 메서드 반환 타입(`ApiResponse<SomeDto>`)에서 스키마를 자동 추론하도록 content를 생략해야 한다.
- 생성 API는 `200`이 아니라 실제 HTTP semantics에 맞게 `201 Created`를 사용한다.
- 구현체의 실제 응답 코드와 Swagger 명세가 다르면 안 된다. 예를 들어 `@ResponseStatus(HttpStatus.CREATED)`를 사용하면 Swagger도 `responseCode = "201"`로 맞춘다.
- 성공 응답 예시는 공통 Swagger customizer(`openApiCustomizer`)가 자동으로 채운다. 수동 예시가 필요한 경우에는 실제 응답 포맷과 동일하게 작성한다.

## Swagger 에러 응답 명세 규칙

Swagger `@ApiResponse`에 에러를 문서화할 때, `GlobalExceptionHandler`가 실제로 반환하는 응답 포맷과 일치하도록 `@Content` + `@Schema`를 반드시 포함한다.

도메인 에러는 개별 `@ExampleObject`를 하드코딩하는 방식보다 `@ApiErrorCodeResponse`, `@ApiErrorCodeResponses`와 `ApiErrorCodeOperationCustomizer` 조합을 우선 사용한다. 새로운 API를 추가할 때도 가능한 한 이 패턴을 따른다.

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

```kotlin
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

// 성공 응답 — content 없이 responseCode와 description만 선언
// SpringDoc이 메서드 반환 타입에서 실제 DTO 스키마를 자동 추론한다
@SwaggerApiResponse(
    responseCode = "200",
    description = "조회 성공",
)

// Validation 에러
@SwaggerApiResponse(
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

// 비즈니스 에러는 StatusIfs enum 기반으로 선언
@ApiErrorCodeResponses(
    [
        ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND"]),
        ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
    ],
)
```

### 주의사항

- Validation 에러처럼 수동 예시가 꼭 필요한 경우에만 `ExampleObject`를 사용한다.
- `ExampleObject`의 `value`는 줄 길이 제한과 가독성을 위해 `companion object`의 `const val`로 분리한다.
- `status` 필드 값은 반드시 해당 도메인의 `StatusIfs` enum에 정의된 `customStatusCode`를 사용한다. 단, 성공 응답은 실제 HTTP status code (`200`, `201`)를 사용한다.
- `message` 필드 값은 해당 `StatusIfs` enum의 `description`과 일치시킨다.
- 한 API에서 발생 가능한 예외는 빠짐없이 선언한다. `401`, `403`, `404`, validation `400`, 도메인 예외, `UNKNOWN_EXCEPTION` 누락을 금지한다.
- 파라미터 설명, DTO 필드 설명, 응답 설명은 모두 한국어로 작성한다.
- Swagger 명세를 수정했으면 관련 Docs interface와 구현체 Controller 시그니처가 여전히 정확히 일치하는지 반드시 확인한다.

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
