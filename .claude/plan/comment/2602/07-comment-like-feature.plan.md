# Comment Like/Dislike Feature Implementation

## Business Goal
댓글에 대한 좋아요/취소 기능을 구현하여 사용자가 댓글에 대한 반응을 표현할 수 있도록 합니다. 게시글 좋아요(PostLikeLog)와 동일한 패턴을 사용하여 코드베이스의 일관성을 유지하고, isLiked=true(좋아요), isLiked=false(취소) 상태를 관리합니다.

## Scope
- **In Scope**:
  - CommentLikeLog 엔티티 생성 (comment, user, isLiked)
  - DB 마이그레이션 및 인덱스 생성
  - CommentLikeLogService (좋아요 생성/업데이트, likeCount 동기화)
  - CommentLikeController (POST /api/comments/{commentId}/like)
  - RecordCommentLikeRequest DTO
  - 통합 테스트 (Given-When-Then 패턴)

- **Out of Scope**:
  - 싫어요 카운트 (dislikeCount 필드)
  - 좋아요한 사용자 목록 조회 API
  - 실시간 알림 기능
  - 좋아요 통계 집계 (PostDailyStats 같은 기능)

## Codebase Analysis Summary
- PostLikeLog 패턴이 이미 존재하며 검증됨 (post, user, isLiked 구조)
- Comment 엔티티에 likeCount 필드가 이미 존재 (V12 마이그레이션에서 추가됨)
- CommentController, CommentReadOpenApiController 분리 패턴 사용 중
- IntegrationTest 기반 테스트 환경 구축 (Testcontainers, PostgreSQL 15)
- JPA Criteria API 사용 (CommentRepositoryCustomImpl)
- Given-When-Then 테스트 패턴 적용 중

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `comment/entity/CommentLikeLog.kt` | 댓글 좋아요 로그 엔티티 | Create |
| `comment/dto/RecordCommentLikeRequest.kt` | 좋아요 요청 DTO | Create |
| `comment/application/CommentLikeLogService.kt` | 좋아요 비즈니스 로직 | Create |
| `comment/infrastructure/out/CommentLikeLogRepository.kt` | 좋아요 저장소 | Create |
| `comment/infrastructure/in/CommentLikeController.kt` | 좋아요 API 컨트롤러 | Create |
| `db/migration/V14__create_comment_like_log.sql` | DB 마이그레이션 | Create |
| `test/.../CommentLikeControllerIntegrationTest.kt` | 통합 테스트 | Create |
| `comment/entity/Comment.kt` | 댓글 엔티티 (likeCount 필드 사용) | Reference |
| `comment/infrastructure/out/CommentRepository.kt` | 댓글 저장소 | Reference |
| `user/infrastructure/out/UserRepository.kt` | 사용자 저장소 | Reference |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 엔티티 네이밍 | PostLikeLog | {Domain}LikeLog 패턴 사용 |
| 테이블 네이밍 | post_like_log | snake_case 사용 |
| 서비스 네이밍 | PostLikeLogService | {Domain}LikeLogService 패턴 |
| 컨트롤러 분리 | PostLikeController | 좋아요 기능은 별도 컨트롤러로 분리 |
| DTO 네이밍 | RecordPostLikeRequest | Record{Domain}LikeRequest 패턴 |
| 인덱스 네이밍 | idx_post_like_log_created | idx_{table}_{columns} 패턴 |
| 유니크 제약조건 | UniqueConstraint(columnNames = ["post_id", "user_id"]) | JPA @Table annotation 사용 |
| 테스트 패턴 | Given-When-Then | 명확한 테스트 구조 |
| 통합 테스트 상속 | IntegrationTest | Testcontainers 기반 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 엔티티 구조 | PostLikeLog 패턴 복제 (comment, user, isLiked) | 일관성, 검증된 패턴 재사용 | Comment에 연관관계 추가 (복잡도 증가) |
| isLiked 의미 | TRUE=좋아요, FALSE=취소 | PostLikeLog와 동일, 싫어요 없음 | NULL 허용 (불필요한 복잡도) |
| 컨트롤러 분리 | CommentLikeController 별도 생성 | PostLikeController와 동일, 단일 책임 원칙 | CommentController에 통합 (책임 분산) |
| likeCount 동기화 | Service 레이어에서 즉시 업데이트 | PostLikeLogService 패턴 유지 | 배치 동기화 (복잡도 증가, 실시간성 하락) |
| 중복 좋아요 처리 | 동일 값이면 무시, 다른 값이면 업데이트 | PostLikeLogService 로직 복제 | 매번 업데이트 (불필요한 쓰기) |
| 인증 처리 | @AuthenticationPrincipal userId | 기존 패턴 유지 | JWT 수동 파싱 (불필요) |

## API Contracts

### POST /api/comments/{commentId}/like
- **Headers**: Authorization: Bearer {JWT token}
- **Path Parameters**:
  - commentId: UUID (댓글 ID)
- **Request**:
  ```json
  {
    "isLiked": true
  }
  ```
- **Response**:
  ```json
  {
    "status": "success",
    "data": null
  }
  ```
- **Status Codes**:
  - 200 OK: 좋아요 기록 성공
  - 400 Bad Request: 잘못된 요청 (validation 실패)
  - 401 Unauthorized: 인증되지 않은 사용자
  - 404 Not Found: 댓글 또는 사용자를 찾을 수 없음
- **Note**:
  - 같은 값으로 다시 호출 시 무시 (idempotent)
  - isLiked=true → false로 변경 시 likeCount -1
  - isLiked=false → true로 변경 시 likeCount +1

## Data Models

### CommentLikeLog
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK (EntityBase 상속) |
| comment | Comment (ManyToOne) | FK(comment_id), NOT NULL |
| user | User (ManyToOne) | FK(user_id), NOT NULL |
| isLiked | Boolean | NOT NULL, default=true |
| createdAt | Timestamp | NOT NULL (EntityBase 상속) |
| updatedAt | Timestamp | NOT NULL (EntityBase 상속) |

- **Unique Constraint**: (comment_id, user_id)
- **Indexes**:
  - idx_comment_like_log_comment_created (comment_id, created_at)
  - idx_comment_like_log_created (created_at)

## Implementation Todos

### Todo 1: Create CommentLikeLog Entity
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 댓글 좋아요 로그 엔티티를 생성하여 데이터 모델을 정의합니다.
- **Work**:
  - `main-server/src/main/kotlin/com/techtaurant/mainserver/comment/entity/CommentLikeLog.kt` 파일 생성
  - PostLikeLog.kt 구조를 참고하여 작성:
    - `@Entity` annotation
    - `@Table(name = "comment_like_log", uniqueConstraints = [UniqueConstraint(columnNames = ["comment_id", "user_id"])], indexes = [...])`
    - `class CommentLikeLog(comment: Comment, user: User, isLiked: Boolean = true) : EntityBase()`
    - `@ManyToOne(fetch = FetchType.LAZY)` for comment, user
    - `@Column(name = "is_liked", nullable = false)` for isLiked
  - KDoc 주석 추가 (한국어):
    - 클래스 설명: "댓글 좋아요 이벤트 로그 엔티티"
    - isLiked 설명: "TRUE: 좋아요, FALSE: 좋아요 취소"
    - 유니크 제약조건 설명: "한 사용자는 같은 댓글에 대해 하나의 좋아요 상태만 가질 수 있습니다."
- **Convention Notes**:
  - EntityBase 상속 (id, createdAt, updatedAt 자동 포함)
  - var 사용 (mutable properties)
  - fetch = FetchType.LAZY (지연 로딩)
  - nullable = false for required fields
- **Verification**:
  - 파일이 생성되고 컴파일 에러가 없는지 확인
  - `./gradlew build` 실행하여 빌드 성공 확인
- **Exit Criteria**:
  - CommentLikeLog.kt 파일이 존재하고 컴파일 가능
  - PostLikeLog와 동일한 구조를 가짐
- **Status**: pending

### Todo 2: Create DB Migration for CommentLikeLog
- **Priority**: 1
- **Dependencies**: none
- **Goal**: comment_like_log 테이블을 생성하는 Flyway 마이그레이션을 작성합니다.
- **Work**:
  - `main-server/src/main/resources/db/migration/V14__create_comment_like_log.sql` 파일 생성
  - PostLikeLog 마이그레이션(V9)을 참고하여 작성:
    ```sql
    CREATE TABLE comment_like_log (
        id UUID PRIMARY KEY,
        comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        is_liked BOOLEAN NOT NULL DEFAULT true,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(comment_id, user_id)
    );

    CREATE INDEX idx_comment_like_log_comment_created ON comment_like_log(comment_id, created_at);
    CREATE INDEX idx_comment_like_log_created ON comment_like_log(created_at);
    ```
  - 주석 추가 (한국어):
    - "댓글 좋아요 이벤트 로그 테이블"
    - "좋아요/취소 이벤트를 기록하여 실시간 통계 집계에 사용합니다."
- **Convention Notes**:
  - 버전 번호: V14 (마지막 마이그레이션이 V13)
  - 테이블명: snake_case
  - 외래키: ON DELETE CASCADE (댓글/사용자 삭제 시 로그도 삭제)
  - 기본값: is_liked=true, timestamps=CURRENT_TIMESTAMP
- **Verification**:
  - 애플리케이션 재시작 시 마이그레이션 성공
  - `./gradlew bootRun` 실행하여 Flyway 마이그레이션 로그 확인
- **Exit Criteria**:
  - V14 마이그레이션 파일 존재
  - 애플리케이션 시작 시 comment_like_log 테이블 생성됨
- **Status**: pending

### Todo 3: Create CommentLikeLogRepository
- **Priority**: 1
- **Dependencies**: none (Todo 1과 병렬 가능)
- **Goal**: CommentLikeLog 조회/저장을 위한 Repository를 생성합니다.
- **Work**:
  - `main-server/src/main/kotlin/com/techtaurant/mainserver/comment/infrastructure/out/CommentLikeLogRepository.kt` 파일 생성
  - PostLikeLogRepository를 참고하여 작성:
    ```kotlin
    interface CommentLikeLogRepository : JpaRepository<CommentLikeLog, UUID> {
        fun findByCommentIdAndUserId(commentId: UUID, userId: UUID): CommentLikeLog?
    }
    ```
  - JpaRepository 상속
  - findByCommentIdAndUserId 메서드 정의 (Spring Data JPA query method)
- **Convention Notes**:
  - interface로 정의
  - JpaRepository<Entity, ID> 상속
  - Spring Data JPA naming convention (findBy{Property}And{Property})
- **Verification**:
  - 컴파일 에러 없음
  - `./gradlew build` 실행하여 빌드 성공 확인
- **Exit Criteria**:
  - CommentLikeLogRepository.kt 파일 존재
  - findByCommentIdAndUserId 메서드 정의됨
- **Status**: pending

### Todo 4: Create RecordCommentLikeRequest DTO
- **Priority**: 1
- **Dependencies**: none (Todo 1-3과 병렬 가능)
- **Goal**: 좋아요 요청 DTO를 생성합니다.
- **Work**:
  - `main-server/src/main/kotlin/com/techtaurant/mainserver/comment/dto/RecordCommentLikeRequest.kt` 파일 생성
  - RecordPostLikeRequest를 참고하여 작성:
    ```kotlin
    data class RecordCommentLikeRequest(
        @field:NotNull(message = "isLiked는 필수입니다.")
        val isLiked: Boolean?,
    )
    ```
  - data class로 정의
  - @field:NotNull validation annotation 추가
  - Boolean? 타입 (nullable) - validation을 위해
- **Convention Notes**:
  - data class (immutable)
  - @field: prefix for validation annotations
  - 한국어 에러 메시지
  - val (immutable property)
- **Verification**:
  - 컴파일 에러 없음
  - `./gradlew build` 실행하여 빌드 성공 확인
- **Exit Criteria**:
  - RecordCommentLikeRequest.kt 파일 존재
  - validation annotation 적용됨
- **Status**: pending

### Todo 5: Create CommentLikeLogService
- **Priority**: 2
- **Dependencies**: Todo 1, 3, 4 (엔티티, 리포지토리, DTO 필요)
- **Goal**: 댓글 좋아요 비즈니스 로직을 구현합니다.
- **Work**:
  - `main-server/src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentLikeLogService.kt` 파일 생성
  - PostLikeLogService를 참고하여 작성:
    ```kotlin
    @Service
    class CommentLikeLogService(
        private val commentLikeLogRepository: CommentLikeLogRepository,
        private val commentRepository: CommentRepository,
        private val userRepository: UserRepository,
    ) {
        @Transactional
        fun recordLike(commentId: UUID, userId: UUID, isLiked: Boolean) {
            // 1. 댓글, 사용자 조회 (존재하지 않으면 예외)
            val comment = commentRepository.findById(commentId).orElseThrow { ... }
            val user = userRepository.findById(userId).orElseThrow { ... }

            // 2. 기존 좋아요 로그 조회
            val existingLog = commentLikeLogRepository.findByCommentIdAndUserId(commentId, userId)

            // 3. 중복 처리 (같은 값이면 무시)
            if (existingLog != null && existingLog.isLiked == isLiked) {
                return
            }

            // 4. 좋아요 로그 생성/업데이트
            val log = existingLog ?: CommentLikeLog(comment, user, isLiked)
            if (existingLog != null) {
                log.isLiked = isLiked
            }
            commentLikeLogRepository.save(log)

            // 5. likeCount 동기화
            updateLikeCount(comment, existingLog, isLiked)
        }

        private fun updateLikeCount(comment: Comment, existingLog: CommentLikeLog?, newIsLiked: Boolean) {
            // PostLikeLogService의 updateLikeCount 로직 복제
            when {
                existingLog == null && newIsLiked -> comment.likeCount++
                existingLog == null && !newIsLiked -> {} // 아무것도 안함
                existingLog != null && existingLog.isLiked && !newIsLiked -> comment.likeCount--
                existingLog != null && !existingLog.isLiked && newIsLiked -> comment.likeCount++
            }
            commentRepository.save(comment)
        }
    }
    ```
  - @Service annotation
  - @Transactional for recordLike
  - 예외 처리: EntityNotFoundException 또는 NoSuchElementException
- **Convention Notes**:
  - Service 레이어는 트랜잭션 관리
  - 비즈니스 로직은 private helper method로 분리
  - Repository를 통한 엔티티 조회/저장
  - PostLikeLogService와 동일한 로직 구조
- **Verification**:
  - 컴파일 에러 없음
  - `./gradlew build` 실행하여 빌드 성공 확인
- **Exit Criteria**:
  - CommentLikeLogService.kt 파일 존재
  - recordLike, updateLikeCount 메서드 구현됨
  - PostLikeLogService와 동일한 로직 구조
- **Status**: pending

### Todo 6: Create CommentLikeController
- **Priority**: 2
- **Dependencies**: Todo 5 (서비스 필요)
- **Goal**: 댓글 좋아요 API 엔드포인트를 생성합니다.
- **Work**:
  - `main-server/src/main/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentLikeController.kt` 파일 생성
  - PostLikeController를 참고하여 작성:
    ```kotlin
    @Tag(name = "Comment", description = "댓글 API")
    @RestController
    @RequestMapping("/api/comments")
    @Validated
    class CommentLikeController(
        private val commentLikeLogService: CommentLikeLogService,
    ) {
        @PostMapping("/{commentId}/like")
        @Operation(summary = "댓글 좋아요/취소", description = "댓글에 대한 좋아요 또는 취소를 기록합니다. 인증된 사용자만 호출 가능합니다.")
        @ApiResponses(...)
        fun recordLike(
            @AuthenticationPrincipal userId: UUID,
            @PathVariable commentId: UUID,
            @Valid @RequestBody request: RecordCommentLikeRequest,
        ): ApiResponse<Unit> {
            commentLikeLogService.recordLike(
                commentId = commentId,
                userId = userId,
                isLiked = request.isLiked!!,
            )
            return ApiResponse.ok(Unit)
        }
    }
    ```
  - @RestController, @RequestMapping
  - @Tag, @Operation, @ApiResponses (Swagger 문서화)
  - @AuthenticationPrincipal for userId
  - @Valid for request validation
- **Convention Notes**:
  - POST 메서드 사용 (좋아요는 상태 변경)
  - ApiResponse<Unit> 반환 (성공/실패만)
  - @Validated on class level
  - Swagger annotation 추가
  - 한국어 설명
- **Verification**:
  - 컴파일 에러 없음
  - `./gradlew bootRun` 실행하여 애플리케이션 시작 확인
  - Swagger UI에서 엔드포인트 확인 (http://localhost:8080/swagger-ui.html)
- **Exit Criteria**:
  - CommentLikeController.kt 파일 존재
  - POST /api/comments/{commentId}/like 엔드포인트 노출됨
  - Swagger 문서에 표시됨
- **Status**: pending

### Todo 7: Create Integration Test
- **Priority**: 3
- **Dependencies**: Todo 1-6 (모든 구현 완료 후)
- **Goal**: 댓글 좋아요 API에 대한 통합 테스트를 작성합니다.
- **Work**:
  - `main-server/src/test/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentLikeControllerIntegrationTest.kt` 파일 생성
  - CommentControllerIntegrationTest를 참고하여 작성:
    ```kotlin
    class CommentLikeControllerIntegrationTest : IntegrationTest() {
        @Test
        fun `댓글 좋아요 성공 - 유효한 요청으로 좋아요 기록`() {
            // Given
            val (user, token) = createUserAndToken()
            val post = createPost(user)
            val comment = createComment(post, user)
            val request = RecordCommentLikeRequest(isLiked = true)

            // When
            val response = RestAssured.given()
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .body(request)
                .`when`()
                .post("/api/comments/${comment.id}/like")

            // Then
            response.then().statusCode(200)
            val updatedComment = commentRepository.findById(comment.id!!).get()
            assertThat(updatedComment.likeCount).isEqualTo(1)
            val log = commentLikeLogRepository.findByCommentIdAndUserId(comment.id!!, user.id!!)
            assertThat(log).isNotNull
            assertThat(log?.isLiked).isTrue()
        }

        @Test
        fun `댓글 좋아요 성공 - 좋아요 취소`() {
            // Given: 이미 좋아요한 상태
            val (user, token) = createUserAndToken()
            val post = createPost(user)
            val comment = createComment(post, user)
            commentLikeLogService.recordLike(comment.id!!, user.id!!, true)
            val request = RecordCommentLikeRequest(isLiked = false)

            // When: 취소 요청
            val response = RestAssured.given()
                .header("Authorization", "Bearer $token")
                .contentType("application/json")
                .body(request)
                .`when`()
                .post("/api/comments/${comment.id}/like")

            // Then: likeCount 감소
            response.then().statusCode(200)
            val updatedComment = commentRepository.findById(comment.id!!).get()
            assertThat(updatedComment.likeCount).isEqualTo(0)
        }

        @Test
        fun `댓글 좋아요 성공 - 중복 좋아요 무시`() {
            // Given: 이미 좋아요한 상태
            // When: 다시 좋아요 요청
            // Then: likeCount 변화 없음
        }

        @Test
        fun `댓글 좋아요 실패 - 인증되지 않은 사용자`() {
            // Given: 토큰 없음
            // When: 좋아요 요청
            // Then: 401 Unauthorized
        }

        @Test
        fun `댓글 좋아요 실패 - 존재하지 않는 댓글`() {
            // Given: 무작위 UUID
            // When: 좋아요 요청
            // Then: 404 Not Found
        }

        @Test
        fun `댓글 좋아요 실패 - isLiked null`() {
            // Given: isLiked=null
            // When: 좋아요 요청
            // Then: 400 Bad Request
        }
    }
    ```
  - IntegrationTest 상속
  - Given-When-Then 패턴
  - RestAssured 사용
  - @Test annotation
  - 성공/실패 케이스 모두 커버
- **Convention Notes**:
  - 한글 테스트 메서드명 (백틱 사용)
  - Given-When-Then 주석
  - assertThat (AssertJ 스타일)
  - RestAssured DSL 사용
- **Verification**:
  - 모든 테스트 통과
  - `./gradlew test --tests CommentLikeControllerIntegrationTest` 실행
  - 테스트 커버리지 80% 이상
- **Exit Criteria**:
  - 6개 이상의 테스트 케이스 작성
  - 모든 테스트 통과 (green)
  - 성공/실패 케이스 모두 커버
- **Status**: pending

## Verification Strategy
전체 구현 완료 후 다음 사항을 검증합니다:

1. **빌드 확인**:
   ```bash
   ./gradlew clean build
   ```
   - 컴파일 에러 없음
   - 모든 테스트 통과

2. **애플리케이션 시작**:
   ```bash
   ./gradlew bootRun
   ```
   - Flyway 마이그레이션 성공
   - 애플리케이션 정상 시작
   - Swagger UI 접근 가능

3. **통합 테스트**:
   ```bash
   ./gradlew test --tests CommentLikeControllerIntegrationTest
   ```
   - 모든 테스트 통과
   - 테스트 커버리지 확인

4. **API 수동 테스트** (Swagger UI 또는 curl):
   - POST /api/comments/{commentId}/like (isLiked=true)
   - POST /api/comments/{commentId}/like (isLiked=false)
   - 중복 요청 테스트
   - 인증 없이 요청 (401 확인)
   - 존재하지 않는 댓글 (404 확인)

5. **DB 확인**:
   - comment_like_log 테이블 생성 확인
   - 유니크 제약조건 확인 (같은 user, comment 조합으로 중복 insert 시도)
   - 인덱스 생성 확인
   - comments.like_count 업데이트 확인

## Progress Tracking
- Total Todos: 7
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-07: Plan created
