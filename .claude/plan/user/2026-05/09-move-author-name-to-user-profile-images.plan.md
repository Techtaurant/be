# Move Author Name To User Profile Images

## Business Goal
SSG/ISR 캐싱에 적합한 정적 콘텐츠 API에서 사용자 표시명처럼 변경 가능한 작성자 데이터를 분리합니다. 클라이언트는 정적 콘텐츠 응답의 `authorId`를 기준으로 `/open-api/users/profile-images`를 호출해 작성자 이름과 프로필 이미지를 함께 조회하고 조합할 수 있어야 합니다.

## Scope
- **In Scope**: `/open-api/users/profile-images` 응답에 `authorName` 추가, v2 게시물/댓글 정적 콘텐츠 DTO에서 `authorName` 제거, 관련 Swagger 설명 갱신, 컴파일 검증
- **Out of Scope**: deprecated v1/open-api 응답 변경, API path 변경, DB 스키마 변경, 인증/권한 정책 변경

## Codebase Analysis Summary
Kotlin/Spring Boot 프로젝트이며 DTO는 `src/main/kotlin/com/techtaurant/mainserver/*/dto`에 위치합니다. 정적 콘텐츠 API는 `PostContentListItemResponse`, `PostContentDetailResponse`, `PostContentAuthorResponse`, `CommentContentListResponse`를 사용하고, 사용자 프로필 이미지 batch API는 `UserProfileImageResponse`와 `UserProfileImageReadService`가 담당합니다. 기존 Swagger 설명은 프로필 이미지만 분리되었다고 안내하므로 작성자 이름도 같은 API에서 조회하도록 문구를 맞췄습니다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/user/dto/UserProfileImageResponse.kt` | 사용자 프로필 이미지 batch API 응답 DTO | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/UserProfileImageReadService.kt` | 사용자 목록 조회 후 프로필 이미지 응답 구성 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentListItemResponse.kt` | v2 게시물 정적 목록 응답 DTO | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostContentAuthorResponse.kt` | v2 게시물 정적 상세 작성자 DTO | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/dto/CommentContentListResponse.kt` | v2 댓글/대댓글 공개 콘텐츠 응답 DTO | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadOpenApiV2ControllerDocs.kt` | v2 게시물 API Swagger 설명 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentReadOpenApiV2ControllerDocs.kt` | v2 댓글 API Swagger 설명 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserPostOpenApiV2ControllerDocs.kt` | 사용자별 v2 게시물 API Swagger 설명 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserOpenApiControllerDocs.kt` | 사용자 profile-images API Swagger 설명 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO naming | `BACKEND.md`, existing DTOs | Request/Response suffix 유지, Swagger `@Schema` 설명 명시 |
| Minimal scope | `CODE_PRINCIPLES.md` | 요구된 필드 이동만 수행하고 speculative 변경 금지 |
| Swagger response wrappers | `CLAUDE.md` | 성공 응답 스키마용 wrapper DTO를 새로 만들지 않음 |
| Kotlin style | Existing source | data class constructor와 companion object 매핑 패턴 유지 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 작성자 이름 제공 위치 | `/open-api/users/profile-images` 응답에 `authorName` 추가 | 작성자 이름과 프로필 이미지는 모두 사용자 프로필 변경 데이터이며 같은 `userId` batch 조회 흐름에 맞음 | 별도 `/profile-summaries` API 추가 |
| 정적 콘텐츠 작성자 필드 | `authorId`만 유지 | SSG/ISR 캐시가 사용자 표시명 변경에 영향받지 않음 | 정적 콘텐츠에 `authorName` 유지 |
| 상세 작성자 DTO | `PostContentAuthorResponse`에서 `name` 제거 | 상세 API에서도 작성자 이름을 동일한 batch API로 조합하도록 계약 통일 | 상세 API만 이름 유지 |

## API Contracts

### GET /open-api/users/profile-images
- Headers: 없음
- Request: `userIds: List<UUID>` query parameter, 최대 100개
- Response: `List<UserProfileImageResponse>`
- Note: 응답 항목은 존재하는 사용자만 포함하며 각 항목은 `userId`, `authorName`, `profileImageUrl`을 포함합니다.

### GET /open-api/v2/posts, GET /open-api/v2/users/{userId}/posts
- Headers: 없음
- Request: 기존 cursor/size/filter query parameter 유지
- Response: `CursorPageResponse<PostContentListItemResponse>`
- Note: 정적 콘텐츠 응답은 `authorId`만 포함하고 작성자 이름은 `/open-api/users/profile-images`에서 조회합니다.

### GET /open-api/v2/posts/{postId}
- Headers: 없음
- Request: `postId: UUID` path variable
- Response: `PostContentDetailResponse`
- Note: `author.id`만 포함하고 작성자 이름은 `/open-api/users/profile-images`에서 조회합니다.

### GET /open-api/v2/posts/{postId}/comments, GET /open-api/v2/comments/{commentId}/replies
- Headers: 없음
- Request: 기존 cursor/size/sort parameter 유지
- Response: `CursorPageResponse<CommentContentListResponse>`
- Note: 공개 댓글 콘텐츠 응답은 `authorId`만 포함하고 작성자 이름은 `/open-api/users/profile-images`에서 조회합니다.

## Data Models

### UserProfileImageResponse
| Field | Type | Constraints |
|-------|------|-------------|
| `userId` | `UUID` | 존재하는 사용자 ID |
| `authorName` | `String` | 사용자 표시명 |
| `profileImageUrl` | `String` | resolver로 계산된 프로필 이미지 URL |

## Implementation Todos

### Todo 1: 사용자 profile-images 응답 확장
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 사용자 batch 조회 API에서 작성자 이름과 프로필 이미지 URL을 함께 반환합니다.
- **Work**:
  - `UserProfileImageResponse`에 `authorName: String` 필드를 추가합니다.
  - `UserProfileImageReadService.getUserProfileImages`에서 `authorName = user.name`을 매핑합니다.
  - `UserOpenApiControllerDocs.getUserProfileImages` 설명과 파라미터 설명을 작성자 이름 포함으로 갱신합니다.
- **Convention Notes**: 기존 `@Schema` 필드 설명 스타일을 유지합니다.
- **Verification**: `./gradlew compileKotlin`
- **Exit Criteria**: `UserProfileImageResponse` 생성자가 모든 호출부에서 컴파일되고 Swagger 설명이 새 계약을 반영합니다.
- **Status**: completed

### Todo 2: v2 정적 콘텐츠 응답에서 작성자 이름 제거
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: v2 게시물/댓글 정적 콘텐츠 API가 변경 가능한 작성자 이름을 직접 반환하지 않도록 합니다.
- **Work**:
  - `PostContentListItemResponse`에서 `authorName` 필드와 매핑을 제거합니다.
  - `PostContentAuthorResponse`에서 `name` 필드와 매핑을 제거합니다.
  - `CommentContentListResponse`에서 `authorName` 필드와 매핑을 제거합니다.
  - v2 게시물/댓글 Swagger 설명에서 작성자 이름도 `/open-api/users/profile-images`로 분리되었음을 명시합니다.
  - 사용자별 v2 게시물 Swagger 설명도 같은 문구로 맞춥니다.
- **Convention Notes**: deprecated 기존 응답 DTO인 `PostListItemResponse`, `PostResponse`, `CommentListResponse`는 변경하지 않습니다.
- **Verification**: `./gradlew compileKotlin`
- **Exit Criteria**: v2 정적 콘텐츠 DTO에 `authorName` 또는 `name` 작성자 표시명 필드가 남지 않고 컴파일됩니다.
- **Status**: completed

### Todo 3: 최종 검증
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 전체 변경이 Kotlin 컴파일과 기존 테스트에 영향을 주지 않는지 확인합니다.
- **Work**:
  - `rg -n "authorName|PostContentAuthorResponse|UserProfileImageResponse"`로 남은 계약을 점검합니다.
  - `./gradlew test`를 실행합니다.
  - 실패 시 변경 범위와 관련된 컴파일/테스트 오류를 수정합니다.
- **Convention Notes**: 테스트 실패가 외부 환경 원인이면 원인을 분리해 기록합니다.
- **Verification**: `./gradlew test`, `./gradlew test -x jacocoTestCoverageVerification`
- **Exit Criteria**: 테스트가 통과하거나, 외부 환경 문제일 경우 변경 자체 컴파일 여부와 실패 원인을 명확히 기록합니다.
- **Status**: completed

## Verification Strategy
- `./gradlew compileKotlin`: PASS
- `./gradlew test`: test execution passed, final build failed at `jacocoTestCoverageVerification` because existing global coverage thresholds are below configured minimums.
- `./gradlew test -x jacocoTestCoverageVerification`: PASS
- v2 정적 콘텐츠 DTO 대상 `authorName` 검색: no matches

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-05-09: Plan created
- 2026-05-09: Todo 1 completed — 사용자 profile-images 응답에 authorName 추가
- 2026-05-09: Todo 2 completed — v2 정적 콘텐츠 응답에서 작성자 이름 제거
- 2026-05-09: Todo 3 completed — compileKotlin 통과, 테스트 실행 통과, 전역 Jacoco coverage verification 기준 미달 확인
- 2026-05-09: Execution complete
