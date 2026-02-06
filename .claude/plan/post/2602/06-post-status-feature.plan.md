# 게시물 상태 관리 기능 (DRAFT/PUBLISHED/PRIVATE)

## Business Goal
게시물 작성 중 임시 저장 기능을 제공하여 사용자가 작성 중인 콘텐츠를 보존할 수 있도록 하고, 비공개 게시물 기능을 통해 게시물 공개 범위를 제어할 수 있도록 합니다. 기존 API의 하위 호환성을 유지하면서 새로운 상태 관리 기능을 추가합니다.

## Scope
- **In Scope**:
  - Post 엔티티에 status 필드 추가 (DRAFT, PUBLISHED, PRIVATE)
  - DB 마이그레이션 스크립트 (default: PUBLISHED)
  - POST /api/posts에 status 파라미터 추가 (default: PUBLISHED)
  - PATCH /api/posts/:id 통합 API (내용 수정 + 상태 전환)
  - GET /api/posts/drafts (내 DRAFT 게시물 목록)
  - 작성자 권한 검증 (DRAFT/PRIVATE는 작성자만 조회/수정/삭제)
  - 기존 조회 API에 PUBLISHED 필터링 추가
  - 빈 제목/본문 기본값 처리 ("새 게시물", "Empty")
  - 통합 테스트 작성

- **Out of Scope**:
  - 자동 삭제 배치
  - 상태 전환 히스토리
  - DRAFT 버전 관리
  - 공유 링크 기능

## Codebase Analysis Summary
- **프레임워크**: Spring Boot + Kotlin + JPA
- **DB**: PostgreSQL
- **아키텍처**: Layered (Controller → Service → Repository → Entity)
- **컨트롤러 분리**: PostController (인증), PostReadOpenApiController (공개)
- **엔티티**: EntityBase 상속 (id, createdAt, updatedAt)
- **DTO 패턴**: CreatePostRequest, UpdatePostRequest, *Response
- **테스트**: BaseIntegrationTest + TestContainers + Given-When-Then

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `post/entity/Post.kt` | 게시물 엔티티 | Modify (status 필드 추가) |
| `post/enums/PostStatus.kt` | 에러 코드 enum | Modify (에러 코드 추가) |
| `post/dto/CreatePostRequest.kt` | 생성 요청 DTO | Modify (status 필드 추가) |
| `post/dto/UpdatePostRequest.kt` | 수정 요청 DTO | Create (새 DTO) |
| `post/dto/PostListItemResponse.kt` | 목록 응답 DTO | Reference (기존 활용) |
| `post/application/PostWriteService.kt` | 게시물 작성 서비스 | Modify (updatePost 추가, 검증 로직) |
| `post/application/PostDetailReadService.kt` | 상세 조회 서비스 | Modify (권한 검증 추가) |
| `post/application/PostListReadService.kt` | 목록 조회 서비스 | Modify (DRAFT 목록 조회 추가) |
| `post/infrastructure/in/PostController.kt` | 인증 필요 컨트롤러 | Modify (PATCH, GET /drafts 추가) |
| `post/infrastructure/out/PostRepository.kt` | Repository | Modify (status 필터링 쿼리) |
| `test/PostControllerTest.kt` | 통합 테스트 | Modify (새 테스트 추가) |
| `db/migration/V{N}__add_post_status.sql` | DB 마이그레이션 | Create |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 네이밍 | CODE_PRINCIPLES.md | 구체적 의미 명시, Info/Data/Manager 금지 |
| DTO | BACKEND.md | Request/Response 접미사, Mapper 사용 |
| Enum | BACKEND.md | 별도 파일로 분리, DB는 Varchar 저장 |
| 주석 | CODE_PRINCIPLES.md | 한국어, 자연스러운 서술형, JavaDoc 스타일 |
| 테스트 | SPRING_BOOT.md | BaseIntegrationTest 상속, Given-When-Then, @DisplayName 한글 |
| SOLID | BACKEND.md | Single Responsibility, 50줄 이하 함수 |
| API | BACKEND.md | RESTful, Swagger Description 한글, 모든 Exception 명시 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 상태 저장 방식 | Post 엔티티에 status Enum 컬럼 | 기존 구조 유지, 단순 마이그레이션, 확장 가능 | DraftPost 별도 테이블 (복잡도 증가) |
| API 통합 | PATCH /api/posts/:id 하나로 통합 | RESTful, 간결, 유연성 | publish/unpublish 분리 (엔드포인트 증가) |
| 기본 상태 | PUBLISHED | 하위 호환성 유지 | DRAFT (기존 동작 변경) |
| 빈 값 처리 | 기본값 자동 설정 | UX 개선, DRAFT 용이성 | 유효성 검증 실패 (불편) |
| 권한 검증 | Service 레이어에서 작성자 확인 | 비즈니스 로직 응집, 재사용 가능 | Controller/Security (분산) |
| 조회 필터링 | Repository 쿼리에 status 조건 | 데이터 레벨 보안, 일관성 | Service 레이어 (누락 위험) |

## API Contracts

### POST /api/posts
- **Headers**: `Authorization: Bearer {token}`
- **Request**:
```json
{
  "title": "string (max 200, optional for DRAFT)",
  "content": "string (optional for DRAFT)",
  "categoryPath": "string (optional)",
  "tags": ["string"] (optional),
  "status": "DRAFT | PUBLISHED | PRIVATE (default: PUBLISHED)"
}
```
- **Response**: 기존 PostResponse
- **Note**: 
  - DRAFT 생성 시 빈 title → "새 게시물", 빈 content → "Empty"
  - PUBLISHED/PRIVATE는 제목/본문 필수

### PATCH /api/posts/:id
- **Headers**: `Authorization: Bearer {token}`
- **Request** (모든 필드 optional):
```json
{
  "title": "string (max 200)",
  "content": "string",
  "categoryPath": "string",
  "tags": ["string"],
  "status": "DRAFT | PUBLISHED | PRIVATE"
}
```
- **Response**: 수정된 PostResponse
- **Note**:
  - 작성자만 수정 가능
  - 포함된 필드만 업데이트 (부분 수정)
  - 상태 전환 시 제목/본문 검증 (DRAFT 제외)

### GET /api/posts/drafts
- **Headers**: `Authorization: Bearer {token}`
- **Query**: `page` (optional, default: 0), `size` (optional, default: 20)
- **Response**:
```json
{
  "content": [PostListItemResponse],
  "totalElements": number,
  "totalPages": number,
  "currentPage": number
}
```
- **Note**: 현재 사용자의 DRAFT 게시물만 반환

### GET /open-api/posts (기존 API 수정)
- **Query**: 기존 파라미터 유지
- **Response**: 기존 응답 유지
- **Note**: PUBLISHED 상태만 필터링 (DRAFT/PRIVATE 제외)

### GET /open-api/posts/:id (기존 API 수정)
- **Response**: 기존 응답 유지
- **Note**: 
  - PUBLISHED만 공개 조회 가능
  - DRAFT/PRIVATE는 작성자만 조회 (인증 필요)

## Data Models

### Post Entity 수정
| Field | Type | Constraints | Note |
|-------|------|-------------|------|
| status | PostStatusEnum | NOT NULL, DEFAULT 'PUBLISHED' | 새 필드 |
| (기존 필드) | ... | ... | 변경 없음 |

### PostStatusEnum (새 Enum)
```kotlin
enum class PostStatusEnum {
    DRAFT,      // 임시 저장
    PUBLISHED,  // 발행됨
    PRIVATE     // 비공개
}
```

### Migration
```sql
ALTER TABLE posts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';
CREATE INDEX idx_posts_status ON posts(status);
CREATE INDEX idx_posts_author_status ON posts(author_id, status);
```

## Implementation Todos

### Todo 1: Post 엔티티 및 Enum 수정
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Post 엔티티에 status 필드를 추가하고 PostStatusEnum을 생성하여 상태 관리 기반을 마련합니다.
- **Work**:
  - `post/enums/PostStatusEnum.kt` 생성
    - DRAFT, PUBLISHED, PRIVATE 값 정의
    - @Enumerated(EnumType.STRING) 사용 준비
  - `post/entity/Post.kt` 수정
    - `status: PostStatusEnum` 필드 추가
    - `@Column(nullable = false)` 설정
    - `@Enumerated(EnumType.STRING)` 어노테이션
    - 기본값: `PostStatusEnum.PUBLISHED`
  - `post/enums/PostStatus.kt` (에러 코드 enum) 수정
    - `CANNOT_MODIFY_OTHERS_POST` 추가 (HttpStatus.FORBIDDEN, 3006)
    - `INVALID_STATUS_TRANSITION` 추가 (HttpStatus.BAD_REQUEST, 3007)
- **Convention Notes**:
  - Enum은 별도 파일로 분리 (BACKEND.md)
  - DB는 VARCHAR로 저장하지만 Enum으로 변환 (BACKEND.md)
  - 주석은 한국어 자연스러운 서술형 (CODE_PRINCIPLES.md)
- **Verification**:
  - 컴파일 성공
  - Post 엔티티 인스턴스 생성 시 status 기본값 확인
- **Exit Criteria**: Post 엔티티에 status 필드가 추가되고 PostStatusEnum이 정의됨
- **Status**: pending

### Todo 2: DB 마이그레이션 스크립트 작성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 기존 posts 테이블에 status 컬럼을 추가하고 기존 데이터를 PUBLISHED로 설정합니다.
- **Work**:
  - `main-server/src/main/resources/db/migration/V{N}__add_post_status.sql` 생성
    - `ALTER TABLE posts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';`
    - `UPDATE posts SET status = 'PUBLISHED' WHERE status IS NULL;`
    - `CREATE INDEX idx_posts_status ON posts(status);`
    - `CREATE INDEX idx_posts_author_status ON posts(author_id, status);`
  - 버전 번호 {N}은 기존 마이그레이션 파일 확인 후 결정
- **Convention Notes**:
  - Flyway 네이밍 규칙: `V{N}__description.sql`
  - 인덱스 네이밍: `idx_{table}_{columns}`
- **Verification**:
  - 애플리케이션 시작 시 마이그레이션 성공
  - `SELECT * FROM posts LIMIT 1;`로 status 컬럼 확인
- **Exit Criteria**: posts 테이블에 status 컬럼이 추가되고 인덱스가 생성됨
- **Status**: pending

### Todo 3: DTO 수정 및 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: CreatePostRequest에 status 필드를 추가하고 UpdatePostRequest DTO를 생성합니다.
- **Work**:
  - `post/dto/CreatePostRequest.kt` 수정
    - `status: PostStatusEnum? = PostStatusEnum.PUBLISHED` 필드 추가
    - `@Schema(description = "게시물 상태 (DRAFT/PUBLISHED/PRIVATE)", example = "PUBLISHED")` 어노테이션
  - `post/dto/UpdatePostRequest.kt` 생성
    ```kotlin
    data class UpdatePostRequest(
        @field:Size(max = 200)
        @field:Schema(description = "게시물 제목", example = "Spring Boot 시작하기")
        val title: String? = null,
        
        @field:Schema(description = "게시물 본문")
        val content: String? = null,
        
        @field:Schema(description = "카테고리 경로", example = "java/spring/deepdive")
        val categoryPath: String? = null,
        
        @field:Schema(description = "태그 목록")
        val tags: List<String>? = null,
        
        @field:Schema(description = "게시물 상태", example = "PUBLISHED")
        val status: PostStatusEnum? = null,
    )
    ```
- **Convention Notes**:
  - Request 접미사 사용 (BACKEND.md)
  - 모든 필드 optional (부분 수정 지원)
  - Description은 한국어 (BACKEND.md)
  - Swagger @Schema 필수 (BACKEND.md)
- **Verification**:
  - 컴파일 성공
  - Swagger UI에서 DTO 스키마 확인
- **Exit Criteria**: CreatePostRequest에 status 필드가 추가되고 UpdatePostRequest가 생성됨
- **Status**: pending

### Todo 4: PostRepository 수정
- **Priority**: 2
- **Dependencies**: Todo 1 (Entity 수정)
- **Goal**: 기존 조회 쿼리에 PUBLISHED 필터를 추가하고 DRAFT 조회 메서드를 추가합니다.
- **Work**:
  - `post/infrastructure/out/PostRepository.kt` 수정
  - `findPostDetailById()` 쿼리 수정
    - status 필터 제거 (Service에서 권한 검증)
  - 새 메서드 추가:
    ```kotlin
    @Query("""
        SELECT p FROM Post p
        WHERE p.author.id = :authorId
        AND p.status = 'DRAFT'
        ORDER BY p.updatedAt DESC
    """)
    fun findDraftsByAuthor(
        @Param("authorId") authorId: UUID,
        pageable: Pageable
    ): Page<Post>
    
    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.author
        LEFT JOIN FETCH p.category
        WHERE p.id = :postId
    """)
    fun findPostByIdWithAuthor(
        @Param("postId") postId: UUID
    ): Post?
    ```
- **Convention Notes**:
  - JPQL 쿼리 사용
  - JOIN FETCH로 N+1 방지
  - 메서드명은 구체적 의미 표현 (CODE_PRINCIPLES.md)
- **Verification**:
  - 컴파일 성공
  - 테스트에서 DRAFT 조회 확인
- **Exit Criteria**: Repository에 DRAFT 조회 메서드가 추가되고 기존 메서드가 수정됨
- **Status**: pending

### Todo 5: PostWriteService 수정 (생성 로직)
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 3
- **Goal**: createPost 메서드에서 status 파라미터를 처리하고 빈 값에 기본값을 설정합니다.
- **Work**:
  - `post/application/PostWriteService.kt` 수정
  - `createPost()` 메서드 수정:
    - `status` 파라미터 처리
    - DRAFT 생성 시 빈 title → "새 게시물", 빈 content → "Empty"
    - PUBLISHED/PRIVATE 생성 시 제목/본문 필수 검증
    ```kotlin
    fun createPost(request: CreatePostRequest, userId: UUID): PostResponse {
        val user = findUserById(userId)
        val status = request.status ?: PostStatusEnum.PUBLISHED
        
        // 빈 값 처리
        val title = if (request.title.isNullOrBlank()) {
            if (status == PostStatusEnum.DRAFT) "새 게시물" else throw ApiException(PostStatus.TITLE_REQUIRED)
        } else request.title
        
        val content = if (request.content.isNullOrBlank()) {
            if (status == PostStatusEnum.DRAFT) "Empty" else throw ApiException(PostStatus.CONTENT_REQUIRED)
        } else request.content
        
        // 기존 로직 + status 설정
        val post = Post(
            title = title,
            content = content,
            author = user,
            status = status,
            // ... 나머지 필드
        )
        // ...
    }
    ```
- **Convention Notes**:
  - 함수는 50줄 이하 유지 (CODE_PRINCIPLES.md)
  - 검증 로직은 명확히 분리
  - 예외는 PostStatus enum 사용 (BACKEND.md)
- **Verification**:
  - 테스트: DRAFT 생성 시 기본값 확인
  - 테스트: PUBLISHED 생성 시 빈 값 예외 확인
- **Exit Criteria**: createPost가 status를 처리하고 빈 값에 기본값을 설정함
- **Status**: pending

### Todo 6: PostWriteService updatePost 메서드 추가
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 3, Todo 4
- **Goal**: 게시물 부분 수정 및 상태 전환을 처리하는 updatePost 메서드를 추가합니다.
- **Work**:
  - `post/application/PostWriteService.kt`에 `updatePost()` 추가
    ```kotlin
    @Transactional
    fun updatePost(postId: UUID, request: UpdatePostRequest, userId: UUID): PostResponse {
        val post = postRepository.findPostByIdWithAuthor(postId)
            ?: throw ApiException(PostStatus.POST_NOT_FOUND)
        
        // 작성자 권한 검증
        if (post.author.id != userId) {
            throw ApiException(PostStatus.CANNOT_MODIFY_OTHERS_POST)
        }
        
        // 부분 수정
        request.title?.let { post.title = it }
        request.content?.let { post.content = it }
        request.categoryPath?.let { post.category = resolveCategory(it) }
        request.tags?.let { post.tags = resolveTags(it) }
        
        // 상태 전환 시 검증
        request.status?.let { newStatus ->
            if (newStatus != PostStatusEnum.DRAFT) {
                if (post.title.isBlank()) throw ApiException(PostStatus.TITLE_REQUIRED)
                if (post.content.isBlank()) throw ApiException(PostStatus.CONTENT_REQUIRED)
            }
            post.status = newStatus
        }
        
        return PostMapper.toResponse(postRepository.save(post))
    }
    ```
  - PostStatus enum에 `TITLE_REQUIRED`, `CONTENT_REQUIRED` 추가
- **Convention Notes**:
  - @Transactional 필수
  - 권한 검증 우선 (보안)
  - let 사용으로 null-safety 보장
- **Verification**:
  - 테스트: 부분 수정 (title만, content만)
  - 테스트: 상태 전환 (DRAFT → PUBLISHED)
  - 테스트: 타인의 게시물 수정 시도 시 예외
- **Exit Criteria**: updatePost 메서드가 추가되고 부분 수정 + 상태 전환을 처리함
- **Status**: pending

### Todo 7: PostController PATCH 엔드포인트 추가
- **Priority**: 3
- **Dependencies**: Todo 6
- **Goal**: PATCH /api/posts/:id 엔드포인트를 추가하여 게시물 수정 API를 제공합니다.
- **Work**:
  - `post/infrastructure/in/PostController.kt` 수정
    ```kotlin
    @PatchMapping("/{postId}")
    @Operation(
        summary = "게시물 수정",
        description = "게시물의 내용을 수정하거나 상태를 전환합니다. 작성자만 수정 가능합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
            ApiResponse(responseCode = "403", description = "권한 없음"),
            ApiResponse(responseCode = "404", description = "게시물 없음")
        ]
    )
    fun updatePost(
        @PathVariable postId: UUID,
        @RequestBody @Valid request: UpdatePostRequest,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Api<PostResponse>> {
        val response = postWriteService.updatePost(postId, request, userId)
        return ResponseEntity.ok(Api.success(response))
    }
    ```
- **Convention Notes**:
  - RESTful API: PATCH for partial update
  - Swagger @Operation 필수, description 한글 (BACKEND.md)
  - 모든 예외 케이스 명시 (BACKEND.md)
- **Verification**:
  - Swagger UI에서 API 스키마 확인
  - Postman/curl로 수동 테스트
- **Exit Criteria**: PATCH /api/posts/:id 엔드포인트가 추가되고 Swagger 문서화됨
- **Status**: pending

### Todo 8: PostListReadService DRAFT 목록 조회 추가
- **Priority**: 2
- **Dependencies**: Todo 4
- **Goal**: 내 DRAFT 게시물 목록을 조회하는 서비스 메서드를 추가합니다.
- **Work**:
  - `post/application/PostListReadService.kt` 수정
    ```kotlin
    @Transactional(readOnly = true)
    fun getMyDrafts(userId: UUID, page: Int, size: Int): Page<PostListItemResponse> {
        val pageable = PageRequest.of(page, size)
        val posts = postRepository.findDraftsByAuthor(userId, pageable)
        return posts.map { PostMapper.toListItemResponse(it) }
    }
    ```
- **Convention Notes**:
  - @Transactional(readOnly = true)
  - 메서드명은 구체적 의미 (getMyDrafts)
  - Mapper 사용 (BACKEND.md)
- **Verification**:
  - 테스트: DRAFT 게시물만 반환
  - 테스트: 다른 사용자의 DRAFT는 제외
- **Exit Criteria**: getMyDrafts 메서드가 추가되고 DRAFT 목록을 반환함
- **Status**: pending

### Todo 9: PostController GET /drafts 엔드포인트 추가
- **Priority**: 3
- **Dependencies**: Todo 8
- **Goal**: GET /api/posts/drafts 엔드포인트를 추가하여 내 DRAFT 목록 조회 API를 제공합니다.
- **Work**:
  - `post/infrastructure/in/PostController.kt` 수정
    ```kotlin
    @GetMapping("/drafts")
    @Operation(
        summary = "내 임시 저장 게시물 목록 조회",
        description = "현재 사용자가 작성한 DRAFT 상태의 게시물 목록을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공")
        ]
    )
    fun getMyDrafts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Api<Page<PostListItemResponse>>> {
        val drafts = postListReadService.getMyDrafts(userId, page, size)
        return ResponseEntity.ok(Api.success(drafts))
    }
    ```
- **Convention Notes**:
  - RESTful: GET for read-only
  - 페이지네이션 기본값 제공
  - Swagger 문서화 (BACKEND.md)
- **Verification**:
  - Swagger UI에서 API 스키마 확인
  - Postman/curl로 수동 테스트
- **Exit Criteria**: GET /api/posts/drafts 엔드포인트가 추가되고 Swagger 문서화됨
- **Status**: pending

### Todo 10: PostDetailReadService 권한 검증 추가
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 4
- **Goal**: DRAFT/PRIVATE 게시물 조회 시 작성자 권한을 검증합니다.
- **Work**:
  - `post/application/PostDetailReadService.kt` 수정
  - `getPostDetail()` 메서드 수정:
    ```kotlin
    @Transactional
    fun getPostDetail(postId: UUID, userId: UUID?, ipAddress: String?, userAgent: String?): PostDetailResponse {
        val post = postRepository.findPostByIdWithAuthor(postId)
            ?: throw ApiException(PostStatus.POST_NOT_FOUND)
        
        // DRAFT/PRIVATE 권한 검증
        if (post.status != PostStatusEnum.PUBLISHED) {
            if (userId == null || post.author.id != userId) {
                throw ApiException(PostStatus.POST_NOT_FOUND) // 404로 감춤
            }
        }
        
        // 기존 로직 (조회 로그 기록 등)
        // ...
    }
    ```
- **Convention Notes**:
  - 보안: 403 대신 404로 존재 여부 감춤
  - 권한 검증 우선 (조회 로그 기록 전)
- **Verification**:
  - 테스트: DRAFT 작성자 조회 성공
  - 테스트: DRAFT 타인 조회 실패 (404)
  - 테스트: PUBLISHED 비회원 조회 성공
- **Exit Criteria**: DRAFT/PRIVATE 게시물 조회 시 작성자 권한 검증이 동작함
- **Status**: pending

### Todo 11: PostListReadService PUBLISHED 필터링 추가
- **Priority**: 2
- **Dependencies**: Todo 4
- **Goal**: 기존 목록 조회 API에서 PUBLISHED만 반환하도록 필터링합니다.
- **Work**:
  - `post/infrastructure/out/PostRepository.kt` 수정
  - 기존 목록 조회 쿼리에 `AND p.status = 'PUBLISHED'` 조건 추가
  - 예: `findPostsFirstPage()`, `findPostsAfterCursor()` 등 (현재 구현 확인 필요)
- **Convention Notes**:
  - 모든 공개 목록 조회는 PUBLISHED만
  - JPQL 쿼리 수정
- **Verification**:
  - 테스트: DRAFT/PRIVATE 게시물은 공개 목록에 미포함
  - 테스트: PUBLISHED만 반환
- **Exit Criteria**: 공개 목록 조회 API에서 PUBLISHED만 반환됨
- **Status**: pending

### Todo 12: 통합 테스트 작성
- **Priority**: 4
- **Dependencies**: Todo 1~11 (모든 구현 완료)
- **Goal**: 새로운 기능에 대한 통합 테스트를 작성하여 동작을 검증합니다.
- **Work**:
  - `test/PostControllerTest.kt` 수정
  - 테스트 케이스 추가:
    1. DRAFT 생성 (빈 제목/본문)
    2. PUBLISHED 생성 (기본값, 하위 호환성)
    3. PATCH: 제목만 수정
    4. PATCH: DRAFT → PUBLISHED 전환
    5. PATCH: 제목 + 상태 동시 수정
    6. GET /drafts: 내 DRAFT 목록 조회
    7. 권한 검증: 타인의 DRAFT 수정 시도 (403)
    8. 권한 검증: 타인의 DRAFT 조회 시도 (404)
    9. 공개 목록: PUBLISHED만 반환
    10. 상태 전환 검증: DRAFT → PUBLISHED (빈 제목 시 예외)
  - 각 테스트는 Given-When-Then 패턴
  - @DisplayName 한글로 명확히 기술
- **Convention Notes**:
  - BaseIntegrationTest 상속 (SPRING_BOOT.md)
  - Given-When-Then 패턴 (SPRING_BOOT.md)
  - @DisplayName 한글 (SPRING_BOOT.md)
  - AssertJ assertThat 사용
- **Verification**:
  - 모든 테스트 통과 (`./gradlew test`)
  - 테스트 커버리지 확인
- **Exit Criteria**: 10개 이상의 통합 테스트가 추가되고 모두 통과함
- **Status**: pending

### Todo 13: Swagger 문서 확인 및 최종 검증
- **Priority**: 5
- **Dependencies**: Todo 12
- **Goal**: Swagger UI에서 API 문서를 확인하고 전체 기능을 수동 테스트합니다.
- **Work**:
  - 애플리케이션 시작
  - Swagger UI 접속 (`/swagger-ui/index.html`)
  - API 문서 확인:
    - POST /api/posts (status 파라미터 확인)
    - PATCH /api/posts/:id (UpdatePostRequest 스키마)
    - GET /api/posts/drafts (response 스키마)
    - 모든 API의 description 한글 확인
    - 예외 케이스 명시 확인
  - Postman/curl 수동 테스트:
    - DRAFT 생성 → 수정 → 발행 플로우
    - 권한 검증 (타인의 DRAFT 접근)
    - 공개 목록에서 DRAFT 제외 확인
- **Convention Notes**:
  - Swagger description 한글 (BACKEND.md)
  - 모든 Exception case 명시 (BACKEND.md)
- **Verification**:
  - Swagger UI 정상 노출
  - 수동 테스트 성공
- **Exit Criteria**: Swagger 문서가 정확하고 모든 API가 정상 동작함
- **Status**: pending

## Verification Strategy
전체 구현 완료 후 검증 방법:
- `./gradlew clean build`: 빌드 성공
- `./gradlew test`: 모든 테스트 통과
- `./gradlew bootRun`: 애플리케이션 정상 시작
- Swagger UI (`/swagger-ui/index.html`): API 문서 확인
- DB 확인: `SELECT * FROM posts LIMIT 10;` status 컬럼 존재 확인
- 통합 시나리오 테스트:
  1. DRAFT 생성 → 수정 → PUBLISHED 전환
  2. PRIVATE 생성 → 공개 목록에서 미노출 확인
  3. 다른 사용자로 DRAFT 조회 시도 → 404

## Progress Tracking
- Total Todos: 13
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-06: Plan created
