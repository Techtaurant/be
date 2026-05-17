# Session Changes - Post Detail API Implementation

Branch: `feat/post-detail`

## Summary
- Changed files (staged): 2 (plan, deleted PostViewController)
- Changed files (unstaged): 2 (PostController, PostRepository)
- Untracked files: 5 (new service, DTO, controller, util)

## Overview
게시물 상세 조회 API 구현 작업. Open API 컨트롤러 분리, 상세 조회 서비스 추가, 조회수/좋아요 여부 기능 포함.

## Diff Statistics

**Staged:**
```
.claude/plan/post/2602/05-post-detail-api.plan.md  | 227 +++++++++++++++
post/infrastructure/in/PostViewController.kt       |  73 ------- (deleted)
```

**Unstaged:**
```
post/infrastructure/in/PostController.kt           | 56 --- (removed PostReadController)
post/infrastructure/out/PostRepository.kt          | 41 +-- (refactored queries)
```

## Files Changed

### Modified Files

#### PostController.kt (Removed PostReadController)
- `PostReadController` 클래스를 별도 파일 `PostReadOpenApiController.kt`로 분리
- 기존 `PostController`는 인증 필요 API만 유지 (게시물 생성)

#### PostRepository.kt (Refactored)
- 제거: `findPostsFirstPage()`, `findPostsAfterCursor()`, `findPostsNeedingStatsSync()`
- 추가: `findPostDetailById(postId: UUID): Post?`
  - author, tags, pictures, category를 JOIN FETCH
  - N+1 문제 방지

### New Files (Untracked)

#### 1. PostDetailReadService.kt
```kotlin
@Service
class PostDetailReadService(
    private val postRepository: PostRepository,
    private val postViewLogService: PostViewLogService,
    private val postLikeLogRepository: PostLikeLogRepository,
) {
    @Transactional
    fun getPostDetail(postId: UUID, userId: UUID?, ipAddress: String?, userAgent: String?): PostDetailResponse
}
```
- 게시물 상세 조회 + 조회 로그 기록
- 좋아요 여부 확인 (로그인 사용자만)

#### 2. PostDetailResponse.kt
```kotlin
data class PostDetailResponse(
    val id: UUID,
    val title: String,
    val content: String,
    val author: AuthorResponse,
    val category: CategoryResponse?,
    val tags: List<PostListTagResponse>,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val isLiked: Boolean,
    val createdAt: Date,
    val updatedAt: Date,
)

data class AuthorResponse(val id: UUID, val name: String, val profileImageUrl: String)
```

#### 3. PostReadOpenApiController.kt
```kotlin
@RestController
@RequestMapping("/open-api/posts")
class PostReadOpenApiController(
    private val postListReadService: PostListReadService,
    private val postDetailReadService: PostDetailReadService,
) {
    @GetMapping fun getPosts(...)  // 목록 조회 (기존 로직)
    @GetMapping("/{postId}") fun getPostDetail(...)  // 상세 조회 (신규)
}
```
- Open API 읽기 전용 컨트롤러 분리
- 비회원도 조회 가능

#### 4. HttpRequestUtils.kt
```kotlin
object HttpRequestUtils {
    fun extractIpAddress(request: HttpServletRequest): String?
}
```
- X-Forwarded-For 헤더에서 IP 추출
- 프록시 환경 지원

## Architecture Decisions

1. **컨트롤러 분리**: 인증 필요/불필요 API를 별도 컨트롤러로 분리
   - `PostController` - 인증 필요 (POST /api/posts)
   - `PostReadOpenApiController` - 인증 불필요 (GET /open-api/posts)

2. **조회 로그 자동 기록**: `getPostDetail()` 호출 시 자동으로 조회 로그 기록
   - IP, User-Agent 수집
   - `PostViewLogService` 위임

3. **좋아요 여부**: 로그인 사용자에 한해 `isLiked` 필드 제공

## TODO / Next Steps
- [ ] 게시물 상세 API 테스트 작성
- [ ] Swagger 문서 확인
- [ ] 조회 로그 서비스 구현 확인

## Full Diff (Unstaged)

```diff
diff --git a/PostController.kt b/PostController.kt
--- PostReadController 클래스 제거 (PostReadOpenApiController로 분리)
--- 불필요한 import 정리

diff --git a/PostRepository.kt b/PostRepository.kt
--- findPostsFirstPage(), findPostsAfterCursor(), findPostsNeedingStatsSync() 제거
+++ findPostDetailById(postId: UUID): Post? 추가
    - author, tags, pictures, category JOIN FETCH
```
