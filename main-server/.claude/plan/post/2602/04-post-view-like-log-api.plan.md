# PostViewLog와 PostLikeLog API 구현

## Business Goal
게시글의 조회 및 좋아요/싫어요 이벤트를 로그로 기록하여 실시간 통계 집계 및 사용자 활동 추적을 가능하게 합니다. 비회원도 조회 로그를 남길 수 있어 정확한 게시글 조회 통계를 수집하며, 회원의 좋아요/싫어요 기록을 통해 게시글 평가 시스템을 구축합니다.

## Scope
- **In Scope**:
  - PostViewLog 생성 API (비인증, 비회원 가능)
  - PostLikeLog 생성/수정 API (인증 필수)
  - Service Layer 구현 (PostViewLogService, PostLikeLogService)
  - DTO 구현 (RecordPostLikeRequest)
  - IP 주소 및 User-Agent 자동 추출
  - PostLikeLog UPDATE 로직 (기존 레코드 존재 시)

- **Out of Scope**:
  - 조회수/좋아요수 집계 로직 (기존 PostStatsService 활용)
  - 중복 조회 방지 로직 (IP 기반 등)
  - 좋아요/싫어요 수 반환 기능
  - 좋아요 취소 기능

## Codebase Analysis Summary

### 기존 구조
- **Entity**: PostViewLog, PostLikeLog는 이미 구현되어 있으며 EntityBase 상속
- **Repository**: PostViewLogRepository, PostLikeLogRepository 이미 존재
- **Controller 패턴**: `/api/*` (인증) vs `/open-api/*` (비인증) 분리
- **인증**: `@AuthenticationPrincipal userId: UUID` 패턴 사용
- **응답 형식**: `ApiResponse<T>` 공통 포맷
- **서비스 네이밍**: `{Domain}{Action}Service` 패턴 (예: PostWriteService)

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `post/entity/PostViewLog.kt` | 조회 로그 Entity | Reference |
| `post/entity/PostLikeLog.kt` | 좋아요 로그 Entity | Reference |
| `post/infrastructure/out/PostViewLogRepository.kt` | 조회 로그 Repository | Reference |
| `post/infrastructure/out/PostLikeLogRepository.kt` | 좋아요 로그 Repository | Reference |
| `post/infrastructure/in/PostViewController.kt` | 조회 로그 컨트롤러 | Create |
| `post/infrastructure/in/PostLikeController.kt` | 좋아요 컨트롤러 | Create |
| `post/application/PostViewLogService.kt` | 조회 로그 서비스 | Create |
| `post/application/PostLikeLogService.kt` | 좋아요 서비스 | Create |
| `post/dto/RecordPostLikeRequest.kt` | 좋아요 요청 DTO | Create |
| `post/entity/Post.kt` | 게시글 Entity | Reference |
| `user/entity/User.kt` | 사용자 Entity | Reference |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 네이밍 - Service | 코드베이스 패턴 | `{Domain}{Action}Service` (예: PostViewLogService) |
| 네이밍 - Controller | PostController 패턴 | `{Domain}{Action}Controller` |
| 네이밍 - Method | 기존 패턴 | 동사 사용 (recordView, recordLike) |
| 네이밍 - DTO | 기존 패턴 | `{Action}{Domain}Request/Response` |
| 주석 | CODE_PRINCIPLES.md | 한국어, 서술형, JavaDoc 형식 |
| DTO 검증 | BACKEND.md | `@field:NotBlank`, `@field:NotNull` 등 검증 어노테이션 필수 |
| Swagger | BACKEND.md | API Description 한국어, 모든 Exception Case 명시 |
| Controller 경로 | 기존 패턴 | 인증: `/api/posts/*`, 비인증: `/open-api/posts/*` |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 컨트롤러 분리 | PostViewController (비인증) + PostLikeController (인증) | 인증 요구사항 차이, 기존 `/api` vs `/open-api` 패턴 일치 | 단일 PostInteractionController |
| PostViewLog 중복 | 매번 새로운 레코드 INSERT | 모든 조회 이벤트 기록, 통계 집계 유리, Entity 구조 일치 | IP 기반 중복 방지 |
| PostLikeLog 중복 | 기존 레코드 UPDATE | UniqueConstraint(post_id, user_id) 위반 방지 | 매번 INSERT (제약조건 위반) |
| IP 추출 방식 | HttpServletRequest 직접 사용 | 기존 Security 레이어 패턴 일치 (CookieHelper 참조) | Custom Annotation/Interceptor |
| User-Agent 추출 | HttpServletRequest.getHeader("User-Agent") | 표준 방식, 추가 의존성 불필요 | 별도 라이브러리 |
| 응답 형식 | ApiResponse<Unit> | 기존 공통 응답 포맷, 단순 성공 응답 | 커스텀 DTO |
| 서비스 레이어 분리 | PostViewLogService, PostLikeLogService 별도 | 단일 책임 원칙, 도메인 명확화 | 단일 PostInteractionService |

## API Contracts

### POST /open-api/posts/{postId}/view
- **인증**: 불필요 (비회원 가능)
- **Headers**:
  - User-Agent (자동 추출)
  - X-Forwarded-For (선택, IP 추출용)
- **Path Parameters**:
  - `postId`: UUID (게시글 ID)
- **Request**: 없음
- **Response**:
  ```json
  {
    "status": 200,
    "data": null,
    "message": "OK"
  }
  ```
- **Exceptions**:
  - 404: 게시글이 존재하지 않음
- **Note**:
  - 비회원도 호출 가능
  - IP 주소는 HttpServletRequest.remoteAddr 또는 X-Forwarded-For 헤더에서 추출
  - User-Agent는 자동 추출
  - 인증된 사용자의 경우 userId 자동 연결 (Optional)

### POST /api/posts/{postId}/like
- **인증**: 필수 (`@AuthenticationPrincipal userId`)
- **Path Parameters**:
  - `postId`: UUID (게시글 ID)
- **Request**:
  ```json
  {
    "isLiked": true  // true: 좋아요, false: 싫어요
  }
  ```
- **Response**:
  ```json
  {
    "status": 200,
    "data": null,
    "message": "OK"
  }
  ```
- **Exceptions**:
  - 401: 인증되지 않은 사용자
  - 404: 게시글이 존재하지 않음
- **Note**:
  - 기존 로그가 있으면 isLiked 값만 업데이트
  - 없으면 새로운 레코드 생성

## Data Models

### PostViewLog (기존 Entity)
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK (UUIDv7) |
| post | Post | FK (not null) |
| user | User | FK (nullable, 비회원 가능) |
| ipAddress | String(45) | nullable |
| userAgent | TEXT | nullable |
| createdAt | Date | not null |
| updatedAt | Date | not null |

### PostLikeLog (기존 Entity)
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK (UUIDv7) |
| post | Post | FK (not null) |
| user | User | FK (not null) |
| isLiked | Boolean | not null, default true |
| createdAt | Date | not null |
| updatedAt | Date | not null |
| **Unique Constraint** | (post_id, user_id) | 한 사용자는 게시글당 하나의 로그만 가능 |

## Implementation Todos

### Todo 1: RecordPostLikeRequest DTO 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: PostLikeLog 생성/수정을 위한 요청 DTO를 작성합니다.
- **Work**:
  - `post/dto/RecordPostLikeRequest.kt` 파일 생성
  - 필드: `isLiked: Boolean` (true: 좋아요, false: 싫어요)
  - `@field:NotNull` 검증 어노테이션 추가
  - `@Schema` 어노테이션으로 Swagger 문서화 (한국어 description)
  - JavaDoc 주석: 비즈니스 의미 명시 (좋아요/싫어요 구분)
- **Convention Notes**:
  - DTO 네이밍: `Record{Domain}{Action}Request` 패턴
  - 검증: `@field:NotNull` 필수
  - 주석: 한국어, 서술형, JavaDoc 형식
- **Verification**: Kotlin 컴파일 성공
- **Exit Criteria**: DTO 클래스 생성 및 컴파일 성공
- **Status**: pending

### Todo 2: PostViewLogService 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 게시글 조회 로그를 생성하는 서비스를 구현합니다.
- **Work**:
  - `post/application/PostViewLogService.kt` 파일 생성
  - `@Service` 어노테이션 추가
  - 의존성: `PostViewLogRepository`, `PostRepository`, `UserRepository` (선택적)
  - 메서드: `recordView(postId: UUID, userId: UUID?, ipAddress: String?, userAgent: String?)`
    - Post 존재 여부 확인 (`postRepository.findById()`)
    - 존재하지 않으면 예외 발생 (적절한 Exception)
    - userId가 있으면 User 조회 (선택적)
    - PostViewLog 엔티티 생성 및 저장
  - 트랜잭션: `@Transactional`
  - 주석: 한국어, 서술형, 파라미터 및 예외 명시
- **Convention Notes**:
  - 서비스 네이밍: `{Domain}{Action}Service`
  - 단일 책임: 조회 로그 생성만 담당
  - 예외 처리: 존재하지 않는 게시글에 대해 명확한 예외
- **Verification**: Kotlin 컴파일 성공, Service Bean 등록 확인
- **Exit Criteria**: PostViewLogService 생성 및 recordView 메서드 구현 완료
- **Status**: pending

### Todo 3: PostLikeLogService 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 게시글 좋아요/싫어요 로그를 생성 또는 수정하는 서비스를 구현합니다.
- **Work**:
  - `post/application/PostLikeLogService.kt` 파일 생성
  - `@Service` 어노테이션 추가
  - 의존성: `PostLikeLogRepository`, `PostRepository`, `UserRepository`
  - 메서드: `recordLike(postId: UUID, userId: UUID, isLiked: Boolean)`
    - Post 존재 여부 확인
    - User 존재 여부 확인
    - 기존 PostLikeLog 조회 (findByPostIdAndUserId - Repository 메서드 추가 필요)
    - 기존 로그가 있으면: `isLiked` 값만 업데이트
    - 기존 로그가 없으면: 새로운 PostLikeLog 생성 및 저장
  - 트랜잭션: `@Transactional`
  - 주석: 한국어, 서술형, UPDATE 로직 설명
- **Convention Notes**:
  - 서비스 네이밍: `{Domain}{Action}Service`
  - 단일 책임: 좋아요 로그 생성/수정만 담당
  - YAGNI: 좋아요수 집계는 포함하지 않음 (기존 PostStatsService 활용)
- **Verification**: Kotlin 컴파일 성공, Service Bean 등록 확인
- **Exit Criteria**: PostLikeLogService 생성 및 recordLike 메서드 구현 완료
- **Status**: pending

### Todo 4: PostLikeLogRepository에 findByPostIdAndUserId 메서드 추가
- **Priority**: 2
- **Dependencies**: Todo 3
- **Goal**: PostLikeLogService에서 기존 로그를 조회할 수 있도록 Repository 메서드를 추가합니다.
- **Work**:
  - `post/infrastructure/out/PostLikeLogRepository.kt` 파일 수정
  - 메서드 추가: `fun findByPostIdAndUserId(postId: UUID, userId: UUID): PostLikeLog?`
  - JavaDoc 주석: 파라미터 및 반환값 설명
- **Convention Notes**:
  - JPA Repository 네이밍 컨벤션: `findBy{Field1}And{Field2}`
  - 주석: 한국어, 서술형
- **Verification**: Kotlin 컴파일 성공
- **Exit Criteria**: Repository 메서드 추가 및 컴파일 성공
- **Status**: pending

### Todo 5: PostViewController 생성
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 비인증 게시글 조회 로그 API를 구현합니다.
- **Work**:
  - `post/infrastructure/in/PostViewController.kt` 파일 생성
  - `@RestController`, `@RequestMapping("/open-api/posts")`, `@Validated` 어노테이션 추가
  - `@Tag(name = "Post", description = "게시물 API")` 추가
  - 의존성: `PostViewLogService`
  - 메서드: `recordView(@PathVariable postId: UUID, request: HttpServletRequest, @AuthenticationPrincipal userId: UUID?)`
    - IP 주소 추출: `request.remoteAddr` (또는 `X-Forwarded-For` 헤더)
    - User-Agent 추출: `request.getHeader("User-Agent")`
    - `postViewLogService.recordView(postId, userId, ipAddress, userAgent)` 호출
    - 반환: `ApiResponse.ok<Unit>()`
  - `@PostMapping("/{postId}/view")` 매핑
  - Swagger: `@Operation`, `@ApiResponses` 추가 (한국어 description, 404 예외 명시)
  - 주석: 한국어, 서술형
- **Convention Notes**:
  - Controller 네이밍: `{Domain}{Action}Controller`
  - 경로: `/open-api/*` (비인증)
  - IP 추출: `request.remoteAddr` 우선, `X-Forwarded-For` 대체
  - 메서드 네이밍: 동사 사용 (recordView)
- **Verification**: Kotlin 컴파일 성공, Swagger UI 확인
- **Exit Criteria**: PostViewController 생성 및 recordView 엔드포인트 구현 완료
- **Status**: pending

### Todo 6: PostLikeController 생성
- **Priority**: 3
- **Dependencies**: Todo 1, Todo 3, Todo 4
- **Goal**: 인증 필수 게시글 좋아요/싫어요 API를 구현합니다.
- **Work**:
  - `post/infrastructure/in/PostLikeController.kt` 파일 생성
  - `@RestController`, `@RequestMapping("/api/posts")`, `@Validated` 어노테이션 추가
  - `@Tag(name = "Post", description = "게시물 API")` 추가
  - 의존성: `PostLikeLogService`
  - 메서드: `recordLike(@AuthenticationPrincipal userId: UUID, @PathVariable postId: UUID, @Valid @RequestBody request: RecordPostLikeRequest)`
    - `postLikeLogService.recordLike(postId, userId, request.isLiked)` 호출
    - 반환: `ApiResponse.ok<Unit>()`
  - `@PostMapping("/{postId}/like")` 매핑
  - Swagger: `@Operation`, `@ApiResponses` 추가 (한국어 description, 401/404 예외 명시)
  - 주석: 한국어, 서술형
- **Convention Notes**:
  - Controller 네이밍: `{Domain}{Action}Controller`
  - 경로: `/api/*` (인증 필수)
  - 인증: `@AuthenticationPrincipal userId: UUID`
  - 메서드 네이밍: 동사 사용 (recordLike)
- **Verification**: Kotlin 컴파일 성공, Swagger UI 확인
- **Exit Criteria**: PostLikeController 생성 및 recordLike 엔드포인트 구현 완료
- **Status**: pending

### Todo 7: Gradle 빌드 및 컴파일 검증
- **Priority**: 4
- **Dependencies**: Todo 1, Todo 2, Todo 3, Todo 4, Todo 5, Todo 6
- **Goal**: 전체 프로젝트가 컴파일되고 빌드되는지 검증합니다.
- **Work**:
  - `./gradlew clean build` 실행
  - 컴파일 에러가 있으면 수정
  - 빌드 성공 확인
- **Convention Notes**: N/A
- **Verification**: Gradle 빌드 성공
- **Exit Criteria**: `./gradlew clean build` 성공
- **Status**: pending

### Todo 8: Swagger UI 문서 검증
- **Priority**: 5
- **Dependencies**: Todo 7
- **Goal**: Swagger UI에서 API 문서가 정상적으로 표시되는지 확인합니다.
- **Work**:
  - 애플리케이션 실행
  - Swagger UI 접속 (`http://localhost:8080/swagger-ui.html`)
  - `/open-api/posts/{postId}/view` 엔드포인트 확인
  - `/api/posts/{postId}/like` 엔드포인트 확인
  - 한국어 description, 예외 케이스 명시 확인
- **Convention Notes**: N/A
- **Verification**: Swagger UI에서 두 엔드포인트 정상 표시
- **Exit Criteria**: Swagger UI 문서 확인 완료
- **Status**: pending

## Verification Strategy
1. **컴파일 검증**: `./gradlew clean build` 성공
2. **Swagger UI 검증**: 두 엔드포인트가 Swagger UI에 정상 표시
3. **수동 API 테스트** (선택):
   - `/open-api/posts/{postId}/view` - 비인증으로 호출 가능, 조회 로그 DB 저장 확인
   - `/api/posts/{postId}/like` - 인증 후 호출, 좋아요 로그 DB 저장 및 UPDATE 확인
4. **DB 확인**: `post_view_log`, `post_like_log` 테이블에 레코드 생성 확인

## Progress Tracking
- Total Todos: 8
- Completed: 8
- Status: Execution complete

## Change Log
- 2026-02-04: Plan created
- 2026-02-04: All todos completed, build successful
