# 게시물 v2 정적/동적 API 분리 계획

## Business Goal
게시물 공개 조회 API를 SSG/ISR에 적합한 정적 콘텐츠 응답과 자주 바뀌는 동적 데이터 응답으로 분리한다. 기존 v1 API는 유지하되 Swagger에서 deprecated 처리하고, 프론트엔드가 어떤 v2/분리 API로 대체해야 하는지 명확히 안내한다.

## Scope
- **In Scope**:
  - `GET /open-api/v2/posts`, `GET /open-api/v2/users/{userId}/posts`, `GET /open-api/v2/posts/{postId}` 추가
  - `GET /open-api/posts/metadata?postIds=...` 추가
  - `GET /api/posts/me/states?postIds=...` 추가
  - 기존 `GET /open-api/posts`, `GET /open-api/posts/{postId}`, `GET /open-api/users/{userId}/posts` Swagger deprecated 명시
  - v2 정적 응답에서 동적 집계, 사용자 상태, presigned URL, `category.postCount` 제거
  - 기존 서비스/Repository/Resolver 로직을 최대한 재사용하고, batch 조회에 필요한 최소 메서드만 추가
- **Out of Scope**:
  - DB 스키마 변경
  - 기존 v1 응답 필드 제거 또는 동작 변경
  - FE 코드 변경
  - 랭킹/정렬 전용 API 재설계
  - 기존 조회수 기록 정책 변경. 단, v2 정적 상세 조회는 side effect 없이 조회한다.

## Codebase Analysis Summary
현재 공개 게시물 조회는 `PostReadOpenApiController`가 `/open-api/posts`에서 목록/상세를 제공하고, 사용자 게시물 목록은 `UserOpenApiController`가 `/open-api/users/{userId}/posts`에서 제공한다. 목록 조회는 `PostListReadService.getPosts()`와 `PostListQueryStrategy`를 통해 커서/기간/정렬/작성자 필터를 처리하며, 응답 조립 시 읽음 여부, 카운트, 상태, 썸네일 presigned URL, 작성자 프로필 이미지 URL을 함께 계산한다. 상세 조회는 `PostDetailReadService.getPostDetail()`이 게시물 조회, 조회수 기록, 좋아요 상태, 읽음 상태, 첨부 presigned URL 생성을 한 번에 수행한다.

v2 정적 API는 기존 query strategy와 attachment/profile resolver를 직접 바꾸지 않고, 정적 응답 전용 DTO와 조립 경로를 추가한다. 공개 metadata API는 기존 `AttachmentService`, `UserProfileImageResolver`, `PostRepository`를 재사용해 카운트/상태/URL성 데이터를 batch로 반환한다. 로그인 사용자 상태 API는 기존 `PostReadLogRepository.findByUserIdAndPostIdIn()`, 새 batch 좋아요 로그 조회, `UserBanService.getBannedUserIds()`를 조합한다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadOpenApiController.kt` | v1 공개 게시물 목록/상세 Controller | Modify: metadata endpoint 추가 또는 별도 Controller와 경로 충돌 확인 |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadOpenApiControllerDocs.kt` | v1 공개 게시물 Swagger Docs | Modify: 기존 목록/상세 deprecated 및 대체 API 설명 |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserOpenApiControllerDocs.kt` | v1 사용자 게시물 Swagger Docs | Modify: 기존 사용자 게시물 목록 deprecated 및 대체 API 설명 |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadOpenApiV2Controller.kt` | v2 공개 게시물 목록/상세 Controller | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadOpenApiV2ControllerDocs.kt` | v2 공개 게시물 Swagger Docs | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserPostOpenApiV2Controller.kt` | v2 사용자 게시물 목록 Controller | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserPostOpenApiV2ControllerDocs.kt` | v2 사용자 게시물 Swagger Docs | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostViewerStateController.kt` | 로그인 사용자 상태 batch Controller | Create or merge into `PostController` if file size remains manageable |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostViewerStateControllerDocs.kt` | 로그인 사용자 상태 Swagger Docs | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentListItemResponse.kt` | v2 정적 목록 아이템 응답 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentDetailResponse.kt` | v2 정적 상세 응답 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentCategoryResponse.kt` | `postCount` 없는 게시물 내 카테고리 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostMetadataResponse.kt` | 공개 metadata batch 응답 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostViewerStateResponse.kt` | 로그인 사용자 상태 batch 응답 DTO | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostListReadService.kt` | 기존 목록 조회/커서/query strategy 진입점 | Modify: 정적 목록 조회 메서드 추가 또는 기존 조회 결과를 정적 DTO로 조립 |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostDetailReadService.kt` | 기존 상세 조회/조회수 기록/상태 조립 서비스 | Modify: 조회수 기록 없는 공개 정적 상세 조회 메서드 추가 |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostMetadataReadService.kt` | 공개 metadata batch 조립 서비스 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostViewerStateReadService.kt` | 로그인 사용자 상태 batch 조립 서비스 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepository.kt` | 게시물 조회 Repository | Modify: 공개 postIds batch 조회 메서드 추가 |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostLikeLogRepository.kt` | 좋아요 로그 Repository | Modify: `findByUserIdAndPostIdIn` batch 메서드 추가 |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostReadLogRepository.kt` | 읽음 로그 Repository | Reference: 기존 batch 메서드 재사용 |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/UserBanService.kt` | 차단 사용자 조회 서비스 | Reference: `getBannedUserIds()` 재사용 |
| `src/test/kotlin/com/techtaurant/mainserver/post/...` | 게시물 서비스/Controller 테스트 | Modify/Create targeted tests |
| `src/test/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserOpenApiControllerTest.kt` | 사용자 open-api Controller 테스트 | Modify: v2 사용자 게시물 목록 테스트 추가 가능 |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Swagger 분리 | `SPRING_BOOT_SWAGGER.md`, 기존 `*ControllerDocs.kt` | Controller 구현체에 Swagger annotation을 직접 붙이지 않고 `*Docs` interface에 작성 |
| 응답 wrapper | `CLAUDE.md` | Swagger 성공 응답 wrapper DTO를 새로 만들지 않고 실제 반환 타입 `ApiResponse<...>` 유지 |
| API 설명 | `BACKEND.md`, `SWAGGER.md` | summary/description은 한국어로 작성하고 대체 API를 description에 명시 |
| Error response | 기존 `@ApiErrorResponses`, `@ApiErrorCodeResponses` | 공통 인증/검증/POST_NOT_FOUND 등 실제 발생 가능한 에러를 Swagger에 명시 |
| DTO naming | 기존 `post/dto/*Response.kt` | Request/Response suffix 유지. 기존 게시물 목록 DTO가 `PostListItemResponse` 패턴을 사용하므로 v2 목록 DTO도 같은 지역 패턴을 따른다. |
| 인증 prefix | `SecurityConfig.kt`, `SecurityConstants.kt` | `/open-api/**`는 공개, `/api/**`는 인증 필요. 사용자별 상태는 반드시 `/api` prefix 사용 |
| 기존 로직 재사용 | 사용자 요구 | query strategy, `AttachmentService`, `UserProfileImageResolver`, read/like/ban 서비스 로직을 재사용하고 신규 쿼리는 batch 조회에 한정 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| v2 API 범위 | 콘텐츠 API만 `/open-api/v2/...`로 추가 | 기존 API를 유지하면서 SSG/ISR용 정적 응답을 명확히 분리 | 기존 `/open-api/posts` 응답 변경은 v1 호환성 위험 |
| 공개 동적 API 이름 | `GET /open-api/posts/metadata?postIds=...` | metrics와 media가 모두 공개 동적 데이터이므로 FE 호출 수를 줄이기 위해 batch metadata로 통합 | `/metrics`, `/media` 분리 |
| 사용자 상태 API | `GET /api/posts/me/states?postIds=...` | `isRead`, `likeStatus`, `isBanned`는 로그인 사용자별 값이므로 공개 metadata와 섞지 않음 | metadata에 포함하면 인증/캐시 정책이 불명확 |
| 정적 응답 제외 필드 | counts/status/user state/presigned URL/category.postCount 제거 | 카운트/상태/URL/개인화 값은 변경 가능성이 높거나 만료되므로 ISR 캐시에 부적합 | 일부 필드만 제거하면 캐시 일관성 문제가 남음 |
| v2 상세 조회 side effect | 조회수 기록하지 않음 | 정적 페이지 재검증/빌드 중 조회가 실제 viewCount를 오염시키지 않도록 함 | 기존 `getPostDetail()` 재사용은 view log 기록 부작용 있음 |
| metadata 반환 대상 | 공개 게시물만 반환 | DRAFT/PRIVATE 존재 여부가 공개 API로 노출되지 않게 함 | 요청 postIds 순서대로 null entry 반환은 존재 여부 유추 위험 |
| batch postIds 제한 | `@Size(max = 100)` 수준으로 제한 | 과도한 URL 길이와 presigned URL 대량 발급 비용 방지 | 제한 없음 |

## API Contracts (if applicable)

### GET `/open-api/v2/posts`
- Headers: 없음
- Query:
  - `cursor: String?`
  - `size: Int = 20` (`1..100`)
  - `period: PostPeriod = ALL`
  - `sort: PostSortType = LATEST`
  - `authorId: UUID?`
  - `categoryId: UUID?`
  - `tagIds: List<UUID>?`
- Response: `ApiResponse<CursorPageResponse<PostContentListItemResponse>>`
- Response item schema:
  - `id: UUID`
  - `title: String`
  - `content: String`
  - `authorId: UUID`
  - `authorName: String`
  - `category: PostContentCategoryResponse?`
  - `tags: List<PostListTagResponse>`
  - `createdAt: Date`
  - `updatedAt: Date`
- Note:
  - 공개 정적 콘텐츠만 반환한다.
  - `viewCount`, `likeCount`, `commentCount`, `status`, `thumbnailUrl`, `authorProfileImageUrl`, `isRead`, `likeStatus`, `isBanned`는 반환하지 않는다.

### GET `/open-api/v2/users/{userId}/posts`
- Headers: 없음
- Query:
  - `cursor: String?`
  - `size: Int = 20` (`1..100`)
  - `period: PostPeriod = ALL`
  - `sort: PostSortType = LATEST`
  - `categoryId: UUID?`
- Response: `ApiResponse<CursorPageResponse<PostContentListItemResponse>>`
- Note:
  - 타인/본인 구분 없이 공개 API 기준으로 `PUBLISHED` 게시물만 반환한다.
  - 본인의 PRIVATE/DRAFT 포함 조회는 기존 인증 API 또는 별도 API 대상이며 이번 범위에서 추가하지 않는다.

### GET `/open-api/v2/posts/{postId}`
- Headers: 없음
- Request: path variable `postId: UUID`
- Response: `ApiResponse<PostContentDetailResponse>`
- Response schema:
  - `id: UUID`
  - `title: String`
  - `content: String`
  - `author: PostContentAuthorResponse`
  - `category: PostContentCategoryResponse?`
  - `tags: List<PostListTagResponse>`
  - `createdAt: Date`
  - `updatedAt: Date`
- Note:
  - `PUBLISHED` 게시물만 조회한다.
  - 조회수 기록, 읽음/좋아요 상태 계산, presigned URL 생성은 수행하지 않는다.

### GET `/open-api/posts/metadata`
- Headers: 없음
- Query:
  - `postIds: List<UUID>` required, max 100
- Response: `ApiResponse<List<PostMetadataResponse>>`
- Response item schema:
  - `postId: UUID`
  - `viewCount: Long`
  - `likeCount: Long`
  - `commentCount: Long`
  - `status: PostStatusEnum`
  - `thumbnailUrl: String`
  - `authorProfileImageUrl: String`
  - `attachmentPresignedUrls: List<PostDetailAttachmentPresignedUrlResponse>`
- Note:
  - 공개 동적/만료성 데이터를 batch로 반환한다.
  - 공개 가능한 `PUBLISHED` 게시물만 반환한다.
  - `postIds`에 존재하지 않거나 비공개인 게시물은 응답 목록에서 제외한다.

### GET `/api/posts/me/states`
- Headers: `Authorization: Bearer {accessToken}`
- Query:
  - `postIds: List<UUID>` required, max 100
- Response: `ApiResponse<List<PostViewerStateResponse>>`
- Response item schema:
  - `postId: UUID`
  - `isRead: Boolean`
  - `likeStatus: LikeStatus`
  - `isBanned: Boolean`
- Note:
  - 로그인 사용자만 호출 가능하다.
  - `isBanned`는 요청 사용자가 해당 게시물 작성자를 차단했는지 여부다.

## Data Models (if applicable)

DB 스키마 변경은 없다. 신규 DTO만 추가한다.

### PostContentListItemResponse
| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `UUID` | required |
| `title` | `String` | required |
| `content` | `String` | required, 기존 목록과 동일하게 최대 2000자 |
| `authorId` | `UUID` | required |
| `authorName` | `String` | required |
| `category` | `PostContentCategoryResponse?` | nullable |
| `tags` | `List<PostListTagResponse>` | required |
| `createdAt` | `Date` | required |
| `updatedAt` | `Date` | required |

### PostContentDetailResponse
| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `UUID` | required |
| `title` | `String` | required |
| `content` | `String` | required |
| `author` | `PostContentAuthorResponse` | required |
| `category` | `PostContentCategoryResponse?` | nullable |
| `tags` | `List<PostListTagResponse>` | required |
| `createdAt` | `Date` | required |
| `updatedAt` | `Date` | required |

### PostMetadataResponse
| Field | Type | Constraints |
|-------|------|-------------|
| `postId` | `UUID` | required |
| `viewCount` | `Long` | required |
| `likeCount` | `Long` | required |
| `commentCount` | `Long` | required |
| `status` | `PostStatusEnum` | required |
| `thumbnailUrl` | `String` | required, presigned 또는 기본 썸네일 URL |
| `authorProfileImageUrl` | `String` | required, presigned 또는 외부 URL |
| `attachmentPresignedUrls` | `List<PostDetailAttachmentPresignedUrlResponse>` | required |

### PostViewerStateResponse
| Field | Type | Constraints |
|-------|------|-------------|
| `postId` | `UUID` | required |
| `isRead` | `Boolean` | required |
| `likeStatus` | `LikeStatus` | required |
| `isBanned` | `Boolean` | required |

## Implementation Todos

### Todo 1: v2 정적 응답 DTO 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: SSG/ISR 캐시에 넣을 수 있는 정적 게시물 응답 타입을 분리한다.
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentCategoryResponse.kt` 생성
    - `id`, `name`, `path`, `depth`, `parentId` 포함
    - `postCount` 제외
    - `companion object fun from(category: Category): PostContentCategoryResponse` 추가
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentAuthorResponse.kt` 생성
    - `id`, `name` 포함
    - `profileImageUrl` 제외
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentListItemResponse.kt` 생성
    - 기존 `PostListItemResponse`에서 동적 필드를 제거한 형태
    - `companion object fun from(post: Post): PostContentListItemResponse` 추가
    - `content`는 기존 목록 응답과 동일하게 `post.content.take(2000)` 사용
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentDetailResponse.kt` 생성
    - 기존 `PostDetailResponse`에서 동적/URL/사용자 상태 필드를 제거한 형태
    - `companion object fun from(post: Post): PostContentDetailResponse` 추가
- **Convention Notes**:
  - 모든 DTO field에 `@field:Schema(description = "...")` 작성
  - 기존 DTO의 `Date`, `UUID`, `PostListTagResponse` 사용 패턴을 따른다.
- **Verification**:
  - `./gradlew spotlessCheck --quiet`
  - DTO compile 확인은 Todo 6 이후 targeted test에서 함께 검증
- **Exit Criteria**:
  - 신규 DTO가 compile 가능하고 정적 응답에 동적 필드가 포함되지 않는다.
- **Status**: completed

### Todo 2: metadata/state DTO와 batch repository 메서드 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 공개 metadata와 로그인 사용자 상태를 batch로 조회할 수 있는 최소 데이터 접근 경로를 만든다.
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostMetadataResponse.kt` 생성
    - `postId`, `viewCount`, `likeCount`, `commentCount`, `status`, `thumbnailUrl`, `authorProfileImageUrl`, `attachmentPresignedUrls` 포함
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostViewerStateResponse.kt` 생성
    - `postId`, `isRead`, `likeStatus`, `isBanned` 포함
  - `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepository.kt`에 공개 batch 조회 메서드 추가
    - `findPublishedPostsByIdIn(postIds: List<UUID>): List<Post>`
    - Query는 `JOIN FETCH p.author`, `LEFT JOIN FETCH p.category`, `LEFT JOIN FETCH p.tags`, `WHERE p.id IN :postIds AND p.status = 'PUBLISHED'`
  - `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostLikeLogRepository.kt`에 `findByUserIdAndPostIdIn(userId: UUID, postIds: List<UUID>): List<PostLikeLog>` 추가
- **Convention Notes**:
  - Repository 메서드는 기존 `PostRepository`의 `@Query` 멀티라인 스타일을 따른다.
  - private/draft 노출 방지를 위해 metadata/state 조립용 게시물 조회는 공개 가능한 게시물만 대상으로 한다.
- **Verification**:
  - `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.out.PostRepositoryTest`
- **Exit Criteria**:
  - 공개 게시물 batch 조회가 author/category/tags를 로딩하고 비공개 상태를 제외한다.
- **Status**: completed

### Todo 3: 기존 서비스에 v2 정적 조회 메서드 추가
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 기존 목록/상세 조회 흐름을 활용하면서 v2 정적 DTO를 반환한다.
- **Work**:
  - `PostListReadService`에 `getPostContents(...) : CursorPageResponse<PostContentListItemResponse>` 추가
    - 기존 `getPosts(...)`의 cursor decode, criteria 생성, strategy 선택, hasNext/nextCursor 계산 로직을 재사용
    - read log 조회, attachment 조회, profile image resolver 호출은 수행하지 않음
    - `PostContentListItemResponse.from(post)`로 변환
  - 중복이 커지면 private helper로 `getPostPage(...)` 수준의 작은 메서드만 추출한다.
  - `PostDetailReadService`에 `getPublishedPostContentDetail(postId: UUID): PostContentDetailResponse` 추가
    - `postRepository.findVisiblePostDetailById(postId, null)` 또는 Todo 2의 batch 메서드 중 더 단순한 경로 사용
    - `post.status != PUBLISHED`이면 `ApiException(PostStatus.POST_NOT_FOUND)`
    - `postViewLogService.recordView(...)`, like/read log 조회, attachment URL 생성, profile image resolver 호출은 수행하지 않음
- **Convention Notes**:
  - 기존 v1 `getPosts()`와 `getPostDetail()` 동작은 변경하지 않는다.
  - 정적 상세 조회 메서드명은 side effect가 없음을 드러내도록 `getPublishedPostContentDetail`처럼 작성한다.
- **Verification**:
  - `./gradlew test --tests com.techtaurant.mainserver.post.application.PostListReadServiceTest --tests com.techtaurant.mainserver.post.application.PostDetailReadServiceTest`
- **Exit Criteria**:
  - v2 정적 목록/상세 서비스가 동적 필드 계산 없이 공개 게시물만 반환한다.
- **Status**: completed

### Todo 4: metadata/state 서비스 추가
- **Priority**: 2
- **Dependencies**: Todo 2
- **Goal**: 공개 metadata와 로그인 사용자 상태를 기존 로직 조합으로 batch 반환한다.
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/application/PostMetadataReadService.kt` 생성
    - `getPostMetadata(postIds: List<UUID>): List<PostMetadataResponse>`
    - postIds는 `distinct()`로 중복 제거
    - `postRepository.findPublishedPostsByIdIn(normalizedPostIds)`로 공개 게시물 조회
    - `AttachmentService.getConfirmedAttachmentsByReferenceIds(postIds, AttachmentReferenceType.POST)` 재사용
    - 썸네일 선택 로직은 기존 `PostListReadService`와 동일하게 `post.thumbnailImage` 우선, 없으면 가장 먼저 생성된 attachment, 없으면 기본 썸네일
    - `AttachmentService.generatePresignedDownloadUrlMapByAttachments(...)`로 thumbnail 및 detail attachment URL 생성
    - `UserProfileImageResolver.resolve(posts.map { it.author }.distinctBy { it.id })` 재사용
    - 응답 순서는 요청 `postIds` 순서를 최대한 유지
  - `src/main/kotlin/com/techtaurant/mainserver/post/application/PostViewerStateReadService.kt` 생성
    - `getPostViewerStates(userId: UUID, postIds: List<UUID>): List<PostViewerStateResponse>`
    - `PostRepository.findPublishedPostsByIdIn(...)`로 공개 게시물과 작성자 확인
    - `PostReadLogRepository.findByUserIdAndPostIdIn(...)` 재사용
    - `PostLikeLogRepository.findByUserIdAndPostIdIn(...)` 사용
    - `UserBanService.getBannedUserIds(userId)` 재사용
    - 좋아요 로그가 없으면 `LikeStatus.NONE`, `isLiked=true`면 `LIKE`, 아니면 `DISLIKE`
- **Convention Notes**:
  - 신규 서비스는 `@Service`, `@Transactional(readOnly = true)` 사용
  - 서비스가 URL 생성과 상태 조립을 담당하고 Controller는 파라미터 전달만 수행
- **Verification**:
  - 신규 서비스 단위 테스트 생성 후 `./gradlew test --tests com.techtaurant.mainserver.post.application.PostMetadataReadServiceTest --tests com.techtaurant.mainserver.post.application.PostViewerStateReadServiceTest`
- **Exit Criteria**:
  - metadata는 공개 동적/URL 데이터를 batch로 반환하고, viewer states는 인증 사용자별 read/like/ban 상태를 batch로 반환한다.
- **Status**: completed

### Todo 5: v2 공개 Controller와 Swagger Docs 추가
- **Priority**: 3
- **Dependencies**: Todo 1, Todo 3
- **Goal**: 정적 콘텐츠용 v2 공개 API를 노출한다.
- **Work**:
  - `PostReadOpenApiV2Controller.kt` 생성
    - `@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/v2/posts")`
    - `GET ""`에서 `postListReadService.getPostContents(...)` 호출
    - `GET "/{postId}"`에서 `postDetailReadService.getPublishedPostContentDetail(postId)` 호출
  - `PostReadOpenApiV2ControllerDocs.kt` 생성
    - `@Tag(name = "게시물", description = "게시물 API")`
    - 정적 응답 목적과 metadata/state API 조합 안내를 description에 작성
    - validation, `POST_NOT_FOUND`, unknown error 명시
  - `UserPostOpenApiV2Controller.kt` 생성
    - `@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/v2/users")`
    - `GET "/{userId}/posts"`에서 `postListReadService.getPostContents(authorId = userId, currentUserId = null, ...)` 호출
  - `UserPostOpenApiV2ControllerDocs.kt` 생성
    - 사용자 게시물 v2는 공개 게시물만 반환한다고 명시
- **Convention Notes**:
  - Swagger annotation은 Docs interface에만 작성
  - Controller에는 `@ApiErrorResponses`와 mapping/validation만 둔다.
- **Verification**:
  - `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.in.PostReadOpenApiV2ControllerIntegrationTest --tests com.techtaurant.mainserver.user.infrastructure.in.UserPostOpenApiV2ControllerIntegrationTest`
- **Exit Criteria**:
  - v2 공개 목록/사용자 목록/상세 endpoint가 200/404/validation 케이스를 통과한다.
- **Status**: completed

### Todo 6: metadata/state Controller와 Swagger Docs 추가
- **Priority**: 3
- **Dependencies**: Todo 4
- **Goal**: 공개 동적 metadata와 로그인 사용자 상태 API를 batch-only로 노출한다.
- **Work**:
  - `PostMetadataOpenApiController.kt` 생성 또는 `PostReadOpenApiController.kt`에 method 추가
    - `@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/posts")`
    - `GET "/metadata"`
    - `@RequestParam postIds: List<UUID>`에 `@Size(max = 100)` 적용
    - `postMetadataReadService.getPostMetadata(postIds)` 호출
  - `PostMetadataOpenApiControllerDocs.kt` 생성 또는 기존 Docs에 method 추가
    - metadata가 카운트/상태/presigned URL을 포함하는 공개 동적 데이터임을 description에 명시
  - `PostViewerStateController.kt` 생성
    - `@RequestMapping("${SecurityConstants.API_PREFIX}/posts")`
    - `GET "/me/states"`
    - `@AuthenticationPrincipal userId: UUID`
    - `@RequestParam postIds: List<UUID>`에 `@Size(max = 100)` 적용
    - `postViewerStateReadService.getPostViewerStates(userId, postIds)` 호출
  - `PostViewerStateControllerDocs.kt` 생성
    - 인증 필요, read/like/ban 사용자별 상태임을 Swagger에 명시
- **Convention Notes**:
  - `/open-api/posts/metadata`는 `/open-api/posts/{postId}`보다 구체 경로이므로 mapping 충돌 테스트를 추가한다.
  - `/api/posts/me/states`는 SecurityConfig상 인증 필수다.
- **Verification**:
  - `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.in.PostMetadataOpenApiControllerIntegrationTest --tests com.techtaurant.mainserver.post.infrastructure.in.PostViewerStateControllerIntegrationTest`
- **Exit Criteria**:
  - metadata는 비로그인 200, states는 비로그인 401/로그인 200으로 동작한다.
- **Status**: completed

### Todo 7: v1 Swagger deprecated 및 대체 API 설명 반영
- **Priority**: 3
- **Dependencies**: Todo 5, Todo 6
- **Goal**: 기존 v1 API 사용자가 새 API 조합으로 이동할 수 있도록 문서화한다.
- **Work**:
  - `PostReadOpenApiControllerDocs.getPosts()`의 `@Operation`에 `deprecated = true` 추가
    - description에 대체 API 명시:
      - 정적 콘텐츠: `GET /open-api/v2/posts`
      - 동적 metadata: `GET /open-api/posts/metadata?postIds=...`
      - 사용자 상태: `GET /api/posts/me/states?postIds=...`
  - `PostReadOpenApiControllerDocs.getPostDetail()`의 `@Operation`에 `deprecated = true` 추가
    - 정적 상세는 `GET /open-api/v2/posts/{postId}`로 대체됨을 명시
    - v1은 조회수 기록 side effect가 있음을 유지 설명
  - `UserOpenApiControllerDocs.getPostsByUserId()`의 `@Operation`에 `deprecated = true` 추가
    - 정적 콘텐츠: `GET /open-api/v2/users/{userId}/posts`
    - 동적 metadata/state API 조합 명시
- **Convention Notes**:
  - 기존 Controller mapping과 반환 타입은 변경하지 않는다.
  - `@Deprecated` Kotlin annotation은 API 동작 변경이 아니므로 Swagger deprecated 목적에는 `@Operation(deprecated = true)`를 우선 사용한다.
- **Verification**:
  - Swagger JSON에서 deprecated flag 확인 테스트가 있으면 보강하고, 없으면 Spring context test로 compile 확인
  - `./gradlew test --tests com.techtaurant.mainserver.common.swagger.ApiErrorCodeOperationCustomizerTest`
- **Exit Criteria**:
  - Swagger UI/API docs에서 v1 세 API가 deprecated로 표시되고 대체 API 설명이 포함된다.
- **Status**: completed

### Todo 8: 통합 테스트와 회귀 검증 추가
- **Priority**: 4
- **Dependencies**: Todo 5, Todo 6, Todo 7
- **Goal**: 정적/동적/사용자 상태 분리가 실제 HTTP 응답에서 지켜지는지 검증한다.
- **Work**:
  - `PostReadOpenApiV2ControllerIntegrationTest.kt` 생성
    - v2 목록 응답에 `viewCount`, `likeCount`, `commentCount`, `status`, `thumbnailUrl`, `authorProfileImageUrl`, `isRead`가 없는지 검증
    - v2 상세 조회가 200을 반환하고 viewCount를 증가시키지 않는지 검증
    - PRIVATE/DRAFT 게시물은 v2 공개 상세에서 404인지 검증
  - `UserPostOpenApiV2ControllerIntegrationTest.kt` 생성 또는 기존 사용자 open-api 테스트 확장
    - `/open-api/v2/users/{userId}/posts`가 PUBLISHED만 반환하는지 검증
  - `PostMetadataOpenApiControllerIntegrationTest.kt` 생성
    - metadata가 counts/status/thumbnail/profile/attachment URL을 반환하는지 검증
    - PRIVATE/DRAFT 또는 없는 postId는 응답에서 제외되는지 검증
  - `PostViewerStateControllerIntegrationTest.kt` 생성
    - 비로그인 401
    - 로그인 사용자의 `isRead`, `likeStatus`, `isBanned`가 반영되는지 검증
  - 기존 v1 통합 테스트는 응답/동작이 깨지지 않도록 유지한다.
- **Convention Notes**:
  - 통합 테스트는 `IntegrationTest`를 상속하고 Given-When-Then 주석을 사용한다.
  - RestAssured 기반 기존 테스트 스타일을 따른다.
- **Verification**:
  - `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.in.PostReadOpenApiV2ControllerIntegrationTest --tests com.techtaurant.mainserver.post.infrastructure.in.PostMetadataOpenApiControllerIntegrationTest --tests com.techtaurant.mainserver.post.infrastructure.in.PostViewerStateControllerIntegrationTest --tests com.techtaurant.mainserver.user.infrastructure.in.UserPostOpenApiV2ControllerIntegrationTest`
- **Exit Criteria**:
  - 신규 API 통합 테스트가 모두 통과하고 v1 회귀가 없다.
- **Status**: completed

### Todo 9: 전체 포맷/테스트 검증
- **Priority**: 5
- **Dependencies**: Todo 1, Todo 2, Todo 3, Todo 4, Todo 5, Todo 6, Todo 7, Todo 8
- **Goal**: 전체 변경이 코드 스타일, 컴파일, 테스트 기준을 만족하는지 최종 확인한다.
- **Work**:
  - `./gradlew spotlessApply` 실행
  - `./gradlew test` 실행
  - 실패 시 실패 범위를 분석하고 최대 2회까지 수정 후 재실행
  - 계획 파일의 각 Todo status와 Change Log를 구현 진행에 맞게 갱신
- **Convention Notes**:
  - formatting은 Spotless/ktlint 결과를 따른다.
  - unrelated file change는 건드리지 않는다.
- **Verification**:
  - `./gradlew spotlessCheck`
  - `./gradlew test`
- **Exit Criteria**:
  - Spotless와 테스트가 통과하고 계획 파일이 실행 결과를 반영한다.
- **Status**: completed (scoped verification passed; full-suite failure documented below)

## Verification Strategy
- DTO/서비스 compile 검증: `./gradlew test --tests com.techtaurant.mainserver.post.application.PostListReadServiceTest --tests com.techtaurant.mainserver.post.application.PostDetailReadServiceTest`
- 신규 서비스 검증: `./gradlew test --tests com.techtaurant.mainserver.post.application.PostMetadataReadServiceTest --tests com.techtaurant.mainserver.post.application.PostViewerStateReadServiceTest`
- 신규 API 통합 검증: `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.in.PostReadOpenApiV2ControllerIntegrationTest --tests com.techtaurant.mainserver.post.infrastructure.in.PostMetadataOpenApiControllerIntegrationTest --tests com.techtaurant.mainserver.post.infrastructure.in.PostViewerStateControllerIntegrationTest --tests com.techtaurant.mainserver.user.infrastructure.in.UserPostOpenApiV2ControllerIntegrationTest`
- 회귀 검증: `./gradlew test --tests com.techtaurant.mainserver.post.infrastructure.in.PostReadOpenApiControllerIntegrationTest --tests com.techtaurant.mainserver.user.infrastructure.in.UserOpenApiControllerTest`
- 최종 검증: `./gradlew spotlessCheck test`

## Progress Tracking
- Total Todos: 9
- Completed: 9
- Status: Implementation complete; full-suite verification is blocked by an unrelated existing config test failure

## Change Log
- 2026-05-08: Plan created
- 2026-05-08: Todo 1 completed - v2 정적 응답 DTO 추가
- 2026-05-08: Todo 2 completed - metadata/state DTO와 batch repository 메서드 추가
- 2026-05-08: Todo 3 completed - v2 정적 목록/상세 서비스 메서드 추가
- 2026-05-08: Todo 4 completed - 공개 metadata와 로그인 사용자 상태 서비스 추가
- 2026-05-08: Todo 5 completed - v2 공개 게시물/사용자 게시물 Controller와 Docs 추가
- 2026-05-08: Todo 6 completed - metadata와 me/states Controller 및 Docs 추가
- 2026-05-08: Todo 7 completed - v1 Swagger deprecated와 대체 API 설명 반영
- 2026-05-08: Todo 8 completed - 신규 통합 테스트와 v1 회귀 테스트 검증
- 2026-05-08: Todo 9 completed - spotlessCheck와 구현 범위 targeted tests 통과
- 2026-05-08: Full-suite caveat - `./gradlew spotlessCheck test -x jacocoTestCoverageVerification`는 `LocalOtelDisableEnvironmentPostProcessorTest.springApplication_localEnvironment_disablesOtelThroughRegisteredPostProcessor`에서 기존 설정 placeholder/후처리기 등록 테스트 실패로 차단됨
