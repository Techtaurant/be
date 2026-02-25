# 게시물 읽음/안읽음 명시적 표시 기능

## Business Goal
사용자가 게시물을 명시적으로 읽음/안읽음 표시할 수 있도록 하여, 자동 조회 기록(PostViewLog)과 별개로 사용자 주도의 읽음 관리 기능을 제공한다.

## Scope
- **In Scope**: PostReadLog 엔티티, Repository, Service, Controller, Request DTO, PostDetailResponse에 isRead 추가, PostListReadService의 isRead 소스를 PostReadLog로 전환
- **Out of Scope**: readCount 집계, 일괄 읽음 처리, 알림 연동, PostViewLog 삭제/변경

## Codebase Analysis Summary
- 기존 `PostLikeLog` 패턴(Entity + Repository + Service + Controller)이 참조 모델
- `PostListItemResponse`에 이미 `isRead` 필드 존재 (현재 PostViewLog 기반)
- `PostDetailResponse`에는 `isRead` 필드 없음 (추가 필요)
- `PostListReadService`가 `postViewLogRepository.findDistinctPostIdsByUserIdAndPostIdIn()`으로 읽음 여부 판단 중 → PostReadLog로 전환 필요
- 인증 필요 API는 `/api/posts` 경로 + `PostController`/`PostLikeController` 패턴
- 공개 API는 `/open-api/posts` 경로 + `PostReadOpenApiController` 패턴

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `post/entity/PostReadLog.kt` | 읽음 기록 엔티티 | Create |
| `post/infrastructure/out/PostReadLogRepository.kt` | 읽음 기록 Repository | Create |
| `post/application/PostReadLogService.kt` | 읽음 토글 비즈니스 로직 | Create |
| `post/dto/RecordPostReadRequest.kt` | 읽음 상태 변경 요청 DTO | Create |
| `post/infrastructure/in/PostReadController.kt` | 읽음 표시 API 컨트롤러 | Create |
| `post/dto/PostDetailResponse.kt` | 상세 응답 DTO | Modify (isRead 추가) |
| `post/application/PostDetailReadService.kt` | 상세 조회 서비스 | Modify (isRead 조회 추가) |
| `post/application/PostListReadService.kt` | 목록 조회 서비스 | Modify (PostReadLog로 전환) |
| `post/entity/PostLikeLog.kt` | 좋아요 로그 엔티티 | Reference |
| `post/infrastructure/in/PostLikeController.kt` | 좋아요 컨트롤러 | Reference |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Entity 구조 | PostLikeLog | EntityBase 상속, @Entity, @Table, unique constraint, index |
| UUID | EntityBase | uuid.v7() 자동 생성 |
| Repository | PostLikeLogRepository | JpaRepository<Entity, UUID> 상속, 쿼리 메서드 정의 |
| Service | PostLikeLogService | @Service, @Transactional, postRepository/userRepository로 존재 검증 |
| Controller | PostLikeController | @Tag, @RestController, /api/posts 경로, @AuthenticationPrincipal, Swagger 어노테이션 |
| DTO | RecordPostLikeRequest | data class, @Schema, @field:NotNull, validation message |
| API 응답 | ApiResponse | ApiResponse.ok(Unit) 또는 ApiResponse.ok(data) |
| 주석 | 전체 | KDoc 스타일, 한국어, @param/@return/@throws |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| API 방식 | POST /api/posts/{postId}/read + body {isRead} | PostLikeController 패턴과 일관성 | PUT/DELETE |
| isRead 소스 | PostReadLog (명시적) | 사용자 의도 기반 읽음 상태 관리 | PostViewLog (자동) |
| 기존 PostViewLog isRead | PostReadLog로 전환 | PostViewLog는 조회 분석용, isRead는 사용자 표시용 | 병행 유지 |
| readCount | 미추가 | 개인 기록용이므로 전체 카운트 불필요 | Post에 추가 |

## API Contracts

### POST /api/posts/{postId}/read
- Headers: Authorization: Bearer {token} (필수)
- Request: `{ "isRead": true }`
- Response: `{ "status": 200, "data": null, "message": "성공" }`
- Note: isRead=true → 읽음 표시 (레코드 생성), isRead=false → 안읽음 표시 (레코드 삭제)

## Data Models

### PostReadLog
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, uuid.v7() |
| post_id | UUID | FK(posts), NOT NULL |
| user_id | UUID | FK(users), NOT NULL |
| created_at | Date | 자동 생성 |
| updated_at | Date | 자동 갱신 |

- Unique Constraint: (post_id, user_id)
- Index: idx_post_read_log_user_post (user_id, post_id) — 목록 조회 시 일괄 확인용

## Implementation Todos

### Todo 1: PostReadLog 엔티티 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 읽음 기록을 저장할 JPA 엔티티를 생성한다
- **Work**:
  - `post/entity/PostReadLog.kt` 파일 생성
  - `PostLikeLog` 패턴을 따라 `@Entity`, `@Table` 정의
  - `post`(ManyToOne, FetchType.LAZY), `user`(ManyToOne, FetchType.LAZY) 필드
  - `uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "user_id"])]`
  - `indexes = [Index(name = "idx_post_read_log_user_post", columnList = "user_id,post_id")]`
  - `EntityBase()` 상속
  - KDoc 주석 한국어로 작성
- **Convention Notes**: PostLikeLog와 동일한 구조, isLiked 필드 없음 (레코드 존재 = 읽음)
- **Verification**: 빌드 성공
- **Exit Criteria**: PostReadLog 엔티티가 PostLikeLog와 동일한 패턴으로 생성됨
- **Status**: completed

### Todo 2: PostReadLogRepository 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: PostReadLog 조회/삭제를 위한 Repository를 생성한다
- **Work**:
  - `post/infrastructure/out/PostReadLogRepository.kt` 파일 생성
  - `JpaRepository<PostReadLog, UUID>` 상속
  - `findByPostIdAndUserId(postId: UUID, userId: UUID): PostReadLog?`
  - `existsByPostIdAndUserId(postId: UUID, userId: UUID): Boolean`
  - `findByUserIdAndPostIdIn(userId: UUID, postIds: List<UUID>): List<PostReadLog>` — 목록 조회 시 일괄 확인용
  - KDoc 주석
- **Convention Notes**: PostLikeLogRepository 패턴 참조
- **Verification**: 빌드 성공
- **Exit Criteria**: Repository 인터페이스가 필요한 쿼리 메서드를 모두 포함
- **Status**: completed

### Todo 3: RecordPostReadRequest DTO 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 읽음 상태 변경 요청 DTO를 생성한다
- **Work**:
  - `post/dto/RecordPostReadRequest.kt` 파일 생성
  - `data class RecordPostReadRequest(val isRead: Boolean)`
  - `@Schema(description = "읽음 상태 변경 요청")`
  - `@field:NotNull`, `@field:Schema` 어노테이션
  - KDoc 주석
- **Convention Notes**: RecordPostLikeRequest와 동일한 패턴
- **Verification**: 빌드 성공
- **Exit Criteria**: DTO가 Swagger 명세와 validation을 포함
- **Status**: completed

### Todo 4: PostReadLogService 생성
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 2
- **Goal**: 읽음 토글 비즈니스 로직을 구현한다
- **Work**:
  - `post/application/PostReadLogService.kt` 파일 생성
  - `@Service` 클래스, 생성자 주입: `postReadLogRepository`, `postRepository`, `userRepository`
  - `@Transactional fun toggleReadStatus(postId: UUID, userId: UUID, isRead: Boolean)`:
    - `postRepository.findById(postId)` 존재 검증 (없으면 `ApiException(PostStatus.POST_NOT_FOUND)`)
    - `userRepository.findById(userId)` 존재 검증
    - `isRead = true`: `findByPostIdAndUserId`로 기존 레코드 확인 → 없으면 생성
    - `isRead = false`: `findByPostIdAndUserId`로 기존 레코드 확인 → 있으면 삭제
  - KDoc 주석 한국어
- **Convention Notes**: PostLikeLogService의 recordLike 패턴 참조, 단 카운트 업데이트 불필요
- **Verification**: 빌드 성공
- **Exit Criteria**: 토글 로직이 멱등성을 보장 (이미 읽음인데 읽음 요청 → 무시, 이미 안읽음인데 안읽음 요청 → 무시)
- **Status**: completed

### Todo 5: PostReadController 생성
- **Priority**: 2
- **Dependencies**: Todo 3, Todo 4
- **Goal**: 읽음 표시/해제 API 엔드포인트를 생성한다
- **Work**:
  - `post/infrastructure/in/PostReadController.kt` 파일 생성
  - `@Tag(name = "Post")`, `@RestController`, `@RequestMapping("/api/posts")`, `@Validated`
  - `PostReadLogService` 생성자 주입
  - `@PostMapping("/{postId}/read") fun toggleReadStatus(...)`:
    - `@AuthenticationPrincipal userId: UUID`
    - `@PathVariable postId: UUID`
    - `@Valid @RequestBody request: RecordPostReadRequest`
    - `return ApiResponse.ok(Unit)`
  - Swagger `@Operation`, `@ApiResponses` (200, 401, 404)
  - KDoc 주석
- **Convention Notes**: PostLikeController와 동일한 패턴
- **Verification**: 빌드 성공
- **Exit Criteria**: POST /api/posts/{postId}/read 엔드포인트가 동작
- **Status**: completed

### Todo 6: PostDetailResponse에 isRead 추가 및 PostDetailReadService 수정
- **Priority**: 2
- **Dependencies**: Todo 2
- **Goal**: 게시물 상세 응답에 isRead 필드를 추가하고 서비스에서 조회한다
- **Work**:
  - `PostDetailResponse`에 `isRead: Boolean` 필드 추가 (@field:Schema)
  - `PostDetailResponse.from()` 메서드에 `isRead` 파라미터 추가
  - `PostDetailReadService` 생성자에 `postReadLogRepository` 추가
  - `getPostDetail()`에서 userId가 있으면 `postReadLogRepository.existsByPostIdAndUserId()` 조회
  - `PostDetailResponse.from(post, likeStatus, isRead)` 호출로 수정
  - KDoc에 `@property isRead` 추가
- **Convention Notes**: likeStatus 조회 패턴과 동일하게 isRead 조회
- **Verification**: 빌드 성공
- **Exit Criteria**: PostDetailResponse에 isRead가 포함되고 로그인 사용자에 대해 정확한 값 반환
- **Status**: completed

### Todo 7: PostListReadService의 isRead 소스를 PostReadLog로 전환
- **Priority**: 2
- **Dependencies**: Todo 2
- **Goal**: 게시물 목록의 isRead를 PostViewLog 대신 PostReadLog 기반으로 전환한다
- **Work**:
  - `PostListReadService` 생성자에서 `postViewLogRepository` → `postReadLogRepository`로 교체
  - `getPosts()`에서 `postViewLogRepository.findDistinctPostIdsByUserIdAndPostIdIn()` 호출을 `postReadLogRepository.findByUserIdAndPostIdIn()` 결과의 postId 추출로 변경
  - `readPostIds` 변수명 유지, 로직만 변경
  - `convertToResponse()` 호출 부분은 변경 없음
- **Convention Notes**: 기존 패턴 유지하면서 데이터 소스만 전환
- **Verification**: 빌드 성공
- **Exit Criteria**: 목록 조회 시 isRead가 PostReadLog 기반으로 정확하게 반환
- **Status**: completed

### Todo 8: Spotless 적용 및 최종 검증
- **Priority**: 3
- **Dependencies**: Todo 1~7
- **Goal**: 코드 포맷팅을 적용하고 전체 빌드를 검증한다
- **Work**:
  - `cd main-server && ./gradlew spotlessApply && ./gradlew spotlessCheck` 실행
  - 빌드 오류 발생 시 수정
- **Convention Notes**: CLAUDE.md Completion Checklist 준수
- **Verification**: spotlessCheck 통과, 빌드 성공
- **Exit Criteria**: spotlessCheck 통과 및 빌드 성공
- **Status**: completed

## Verification Strategy
- `./gradlew spotlessApply && ./gradlew spotlessCheck` 통과
- `./gradlew build` 또는 `./gradlew compileKotlin` 통과
- 새 파일들이 기존 패턴(PostLikeLog 계열)과 일관성 있는지 확인

## Progress Tracking
- Total Todos: 8
- Completed: 8
- Status: Execution complete

## Change Log
- 2026-02-22: Plan created
- 2026-02-22: All todos completed, spotlessCheck and compileKotlin passed
