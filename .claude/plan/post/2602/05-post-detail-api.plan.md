# Post Detail API

## Business Goal
게시물 상세 조회 API를 구현하여 사용자가 게시물의 전체 내용(본문, 작성자 프로필, 사진, 태그, 카테고리, 통계)을 조회할 수 있도록 한다.
조회 시 자동으로 PostViewLog를 기록하여 조회수 통계를 지원하고, 기존 독립적인 PostViewController는 삭제하여 API 구조를 정리한다.

## Scope
- **In Scope**:
  - `PostDetailResponse` DTO 생성 (작성자 프로필, 사진, 태그, 카테고리, 통계 포함)
  - `PostDetailReadService` 생성 (상세 조회 + 조회 로그 기록)
  - `PostReadController`에 `GET /open-api/posts/{postId}` 엔드포인트 추가
  - `PostRepository`에 fetch join 쿼리 추가 (N+1 방지)
  - `PostViewController` 삭제
- **Out of Scope**:
  - 댓글 목록 조회 (별도 API)
  - 게시물 수정/삭제 API
  - 테스트 코드

## Codebase Analysis Summary
Post 도메인은 계층형 구조로 infrastructure/in(Controller), application(Service), dto, entity, enums, infrastructure/out(Repository)로 구성되어 있다.
읽기 전용 API는 `/open-api/posts`(PostReadController), 인증 필요 API는 `/api/posts`(PostController)로 분리되어 있다.
PostViewLogService가 이미 조회 로그 기록 기능을 제공하고 있으며, PostViewController에서 별도로 호출하는 구조이나 참조하는 곳이 없다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `post/dto/PostDetailResponse.kt` | 상세 조회 응답 DTO | Create |
| `post/application/PostDetailReadService.kt` | 상세 조회 서비스 | Create |
| `post/infrastructure/in/PostController.kt` | PostReadController에 엔드포인트 추가 | Modify |
| `post/infrastructure/out/PostRepository.kt` | fetch join 쿼리 추가 | Modify |
| `post/infrastructure/in/PostViewController.kt` | 독립 조회 로그 컨트롤러 | Delete |
| `post/application/PostViewLogService.kt` | 조회 로그 기록 서비스 | Reference |
| `post/entity/Post.kt` | 게시물 엔티티 | Reference |
| `post/entity/PostPicture.kt` | 게시물 사진 엔티티 | Reference |
| `post/entity/Category.kt` | 카테고리 엔티티 | Reference |
| `post/entity/Tag.kt` | 태그 엔티티 | Reference |
| `user/entity/User.kt` | 사용자 엔티티 (프로필) | Reference |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO 네이밍 | PostListItemResponse, PostResponse | `Post{Context}Response` 형식 |
| Swagger Schema | PostListItemResponse | `@field:Schema(description = "...")` |
| 팩토리 메서드 | PostResponse, CategoryResponse | `companion object { fun from(...) }` |
| 컨트롤러 구조 | PostReadController | `@Tag`, `@Validated`, `@Operation`, `@ApiResponses` |
| 서비스 구조 | PostListReadService | `@Service`, `@Transactional(readOnly = true)` |
| Repository 쿼리 | PostRepository | `@Query` + `JOIN FETCH` for N+1 방지 |
| 에러 처리 | PostViewLogService | `ApiException(PostStatus.POST_NOT_FOUND)` |
| 비회원 지원 | PostReadController | `@AuthenticationPrincipal userId: UUID?` (nullable) |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 엔드포인트 위치 | PostReadController | open-api 읽기 컨트롤러에 일관성 유지 | 새 컨트롤러 생성 |
| 서비스 분리 | 새 PostDetailReadService | SRP: 목록 vs 상세 조회 분리 | PostListReadService에 추가 |
| 응답 DTO | 새 PostDetailResponse | 상세 조회는 사진, 통계, 작성자 프로필 등 추가 필드 포함 | 기존 PostResponse 확장 |
| 조회 로그 기록 | 서비스 레이어에서 PostViewLogService 호출 | 컨트롤러 간소화, 트랜잭션 관리 용이 | 컨트롤러에서 직접 호출 |
| N+1 방지 | PostRepository에 fetch join 쿼리 | author, tags, pictures, category를 한번에 로딩 | EntityGraph |
| 좋아요 여부 | PostLikeLogRepository 활용 | 로그인 사용자의 좋아요 여부 표시 | 별도 API로 분리 |

## API Contracts

### GET /open-api/posts/{postId}
- Headers: `Authorization: Bearer {token}` (optional, 비회원 조회 가능)
- Request: Path variable `postId` (UUID)
- Response:
```json
{
  "status": 200,
  "data": {
    "id": "uuid",
    "title": "주니어 개발자의 이직 회고",
    "content": "들어가며\n저는 올해 초...",
    "author": {
      "id": "uuid",
      "name": "김개발",
      "profileImageUrl": "https://..."
    },
    "category": {
      "id": "uuid",
      "name": "spring",
      "path": "java/spring",
      "depth": 2,
      "parentId": "uuid"
    },
    "tags": [
      { "id": "uuid", "name": "React" }
    ],
    "pictures": [
      { "id": "uuid", "pictureUrl": "https://...", "isThumbnail": true, "displayOrder": 0 }
    ],
    "viewCount": 56,
    "likeCount": 12,
    "commentCount": 0,
    "isLiked": false,
    "createdAt": "2025-01-16T...",
    "updatedAt": "2025-01-16T..."
  },
  "message": "성공"
}
```
- Note: 비회원인 경우 `isLiked`는 항상 `false`

## Data Models

### PostDetailResponse
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | 게시물 ID |
| title | String | 제목 |
| content | String | 본문 (전체) |
| author | AuthorResponse | 작성자 정보 |
| category | CategoryResponse? | 카테고리 정보 |
| tags | List<PostListTagResponse> | 태그 목록 |
| pictures | List<PostPictureResponse> | 사진 목록 |
| viewCount | Long | 조회수 |
| likeCount | Long | 좋아요수 |
| commentCount | Long | 댓글수 |
| isLiked | Boolean | 현재 사용자 좋아요 여부 |
| createdAt | Date | 작성일 |
| updatedAt | Date | 수정일 |

### AuthorResponse (PostDetailResponse 내부)
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | 작성자 ID |
| name | String | 작성자 이름 |
| profileImageUrl | String | 프로필 이미지 URL |

### PostPictureResponse (PostDetailResponse 내부)
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | 사진 ID |
| pictureUrl | String | 사진 URL |
| isThumbnail | Boolean | 썸네일 여부 |
| displayOrder | Int | 표시 순서 |

## Implementation Todos

### Todo 1: PostDetailResponse DTO 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 게시물 상세 조회에 필요한 응답 DTO를 생성한다
- **Work**:
  - `post/dto/PostDetailResponse.kt` 파일 생성
  - `PostDetailResponse` data class: id, title, content, author(AuthorResponse), category(CategoryResponse?), tags(List<PostListTagResponse>), pictures(List<PostPictureResponse>), viewCount, likeCount, commentCount, isLiked, createdAt, updatedAt
  - `AuthorResponse` data class: id, name, profileImageUrl (같은 파일 내)
  - `PostPictureResponse` data class: id, pictureUrl, isThumbnail, displayOrder (같은 파일 내)
  - 모든 필드에 `@field:Schema(description = "...")` 적용
  - `PostDetailResponse`에 `companion object { fun from(post, isLiked) }` 팩토리 메서드 포함
  - `AuthorResponse`, `PostPictureResponse`에도 각각 `companion object { fun from(...) }` 포함
  - 기존 `CategoryResponse.from(category)`, `PostListTagResponse.from(tag)` 재사용
- **Convention Notes**: Swagger Schema 어노테이션, data class 사용, KDoc 주석
- **Verification**: 빌드 성공
- **Exit Criteria**: DTO 파일이 존재하고 컴파일 가능
- **Status**: pending

### Todo 2: PostRepository에 상세 조회 쿼리 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Post 상세 조회 시 N+1 문제를 방지하는 fetch join 쿼리를 추가한다
- **Work**:
  - `PostRepository.kt`에 `findPostDetailById(postId: UUID): Post?` 메서드 추가
  - `@Query`로 `JOIN FETCH p.author`, `LEFT JOIN FETCH p.tags`, `LEFT JOIN FETCH p.pictures`, `LEFT JOIN FETCH p.category` 적용
  - Optional이 아닌 nullable Post 반환 (Kotlin 관례)
- **Convention Notes**: 기존 `findPostsFirstPage` 쿼리 스타일 참고
- **Verification**: 빌드 성공
- **Exit Criteria**: Repository 메서드가 존재하고 컴파일 가능
- **Status**: pending

### Todo 3: PostDetailReadService 생성
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 2
- **Goal**: 게시물 상세 조회 비즈니스 로직과 조회 로그 기록을 담당하는 서비스를 생성한다
- **Work**:
  - `post/application/PostDetailReadService.kt` 파일 생성
  - 의존성 주입: `PostRepository`, `PostViewLogService`, `PostLikeLogRepository`
  - `getPostDetail(postId: UUID, userId: UUID?, ipAddress: String?, userAgent: String?): PostDetailResponse` 메서드 구현
    - `postRepository.findPostDetailById(postId)` 호출, 없으면 `ApiException(PostStatus.POST_NOT_FOUND)`
    - `postViewLogService.recordView(postId, userId, ipAddress, userAgent)` 호출하여 조회 로그 기록
    - 로그인 사용자인 경우 `postLikeLogRepository.findByPostIdAndUserId(postId, userId)`로 좋아요 여부 확인
    - `PostDetailResponse.from(post, isLiked)` 반환
  - `@Service`, `@Transactional` (readOnly가 아님 - 조회 로그 기록 때문)
- **Convention Notes**: PostListReadService 구조 참고, 에러 처리 패턴 준수
- **Verification**: 빌드 성공
- **Exit Criteria**: 서비스 클래스가 존재하고 컴파일 가능
- **Status**: pending

### Todo 4: PostReadController에 상세 조회 엔드포인트 추가
- **Priority**: 3
- **Dependencies**: Todo 3
- **Goal**: PostReadController에 GET /open-api/posts/{postId} 엔드포인트를 추가한다
- **Work**:
  - `PostReadController`에 `PostDetailReadService` 의존성 추가
  - `getPostDetail(@PathVariable postId: UUID, request: HttpServletRequest, @AuthenticationPrincipal userId: UUID?): ApiResponse<PostDetailResponse>` 메서드 추가
  - IP 추출 로직: 기존 PostViewController의 `extractIpAddress` 로직 활용 (private 메서드로 이동)
  - `@GetMapping("/{postId}")`, `@Operation`, `@ApiResponses` (200, 404) Swagger 명세
- **Convention Notes**: 기존 `getPosts` 메서드의 Swagger 스타일, PostLikeController의 PathVariable 패턴 참고
- **Verification**: 빌드 성공
- **Exit Criteria**: 엔드포인트가 존재하고 Swagger UI에서 확인 가능
- **Status**: pending

### Todo 5: PostViewController 삭제
- **Priority**: 3
- **Dependencies**: Todo 4
- **Goal**: 더 이상 필요 없는 PostViewController를 삭제하여 코드 정리
- **Work**:
  - `post/infrastructure/in/PostViewController.kt` 파일 삭제
  - 참조하는 코드가 없음을 확인 완료 (find_referencing_symbols 결과 빈 배열)
- **Convention Notes**: N/A
- **Verification**: 빌드 성공
- **Exit Criteria**: 파일이 삭제되고 빌드에 영향 없음
- **Status**: pending

## Verification Strategy
- `./gradlew :main-server:compileKotlin` 빌드 성공 확인
- Swagger 명세에서 `GET /open-api/posts/{postId}` 엔드포인트 존재 확인
- PostViewController 삭제 후 빌드 오류 없음 확인

## Progress Tracking
- Total Todos: 5
- Completed: 5
- Status: Execution complete

## Change Log
- 2026-02-05: Plan created
- 2026-02-05: Execution complete - all 5 todos completed successfully
