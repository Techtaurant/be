## Completion Checklist

코드 변경 작업 완료 시 반드시 아래를 실행하고 통과해야 한다.

```bash
cd main-server && ./gradlew spotlessApply && ./gradlew spotlessCheck
```

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
