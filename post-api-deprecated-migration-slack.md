안녕하세요! `origin/dev` 기준 게시물 공개 조회 API가 SSG/ISR 대응을 위해 정적 콘텐츠, 공개 metadata, 로그인 사용자 상태 API로 분리되었습니다.

아래 v1 API는 deprecated 처리되었습니다.

- `GET /open-api/posts`: 게시물 목록 조회
- `GET /open-api/posts/{postId}`: 게시물 상세 조회
- `GET /open-api/users/{userId}/posts`: 사용자 게시물 목록 조회

## 연동 API

**게시물 목록**
- 기존: `GET /open-api/posts`
- 신규 정적 콘텐츠: `GET /open-api/v2/posts`
- 함께 호출: `GET /open-api/posts/metadata?postIds=...`
- 로그인 사용자만 함께 호출: `GET /api/posts/me/states?postIds=...`

**게시물 상세**
- 기존: `GET /open-api/posts/{postId}`
- 신규 정적 콘텐츠: `GET /open-api/v2/posts/{postId}`
- 함께 호출: `GET /open-api/posts/metadata?postIds=...`
- 로그인 사용자만 함께 호출: `GET /api/posts/me/states?postIds=...`
- 상세 진입 조회수 기록: `POST /open-api/posts/{postId}/view-logs`

**사용자 게시물 목록**
- 기존: `GET /open-api/users/{userId}/posts`
- 신규 정적 콘텐츠: `GET /open-api/v2/users/{userId}/posts`
- 함께 호출: `GET /open-api/posts/metadata?postIds=...`
- 로그인 사용자만 함께 호출: `GET /api/posts/me/states?postIds=...`

## FE 처리 포인트

- v2 정적 콘텐츠 API 응답에는 `viewCount`, `likeCount`, `commentCount`, `status`, 썸네일/프로필/첨부 presigned URL, `isRead`, `likeStatus`, `isBanned`가 포함되지 않습니다.
- 목록/상세 응답에서 `postId`를 모은 뒤 `GET /open-api/posts/metadata?postIds=...` 결과를 `postId` 기준으로 병합해 주세요.
- 로그인 상태에서는 `GET /api/posts/me/states?postIds=...`를 추가 호출해서 `isRead`, `likeStatus`, `isBanned`를 병합해 주세요. 비로그인 상태에서는 호출하지 않아도 됩니다.
- v2 상세 조회는 조회 로그를 자동 기록하지 않습니다. 실제 상세 페이지 진입 시 `POST /open-api/posts/{postId}/view-logs`를 별도로 호출해 주세요.
- `postIds` batch 조회는 최대 100개까지 전달할 수 있습니다.
