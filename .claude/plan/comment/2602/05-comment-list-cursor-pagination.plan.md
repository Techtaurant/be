# Comment List API - Cursor-based Pagination

## Business Goal
특정 게시물의 댓글 목록을 커서 기반 페이지네이션으로 조회하는 API를 구현한다. 2단계 API 구조로 부모 댓글과 대댓글을 각각 페이지네이션하며, 추천수/최신순/답글순 정렬 옵션을 지원한다.

## Scope
- **In Scope**:
  - 부모 댓글 목록 API (depth=0, 커서 페이지네이션)
  - 대댓글 목록 API (특정 부모의 대댓글, depth=1, 커서 페이지네이션)
  - 정렬 옵션: LATEST(최신순), LIKE(추천수), REPLY(답글순)
  - Comment 엔티티에 likeCount, replyCount 필드 추가
  - 기존 API를 /open-api/comments로 변경
- **Out of Scope**:
  - 댓글 좋아요 기능 구현 (likeCount 업데이트 로직)
  - 댓글 삭제/수정 API
  - 인증 필요 API 유지

## Codebase Analysis Summary
기존 PostListReadService의 커서 기반 페이지네이션 패턴을 Comment에 적용. 기존 CommentController, CommentReadService, CommentRepository를 수정하고 QueryDSL 커스텀 구현 추가.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| comment/entity/Comment.kt | 댓글 엔티티 | Modify - likeCount, replyCount 필드 추가 |
| comment/enums/CommentSortType.kt | 정렬 타입 enum | Create |
| comment/dto/CommentCursor.kt | 커서 DTO | Create |
| comment/dto/CommentListResponse.kt | 목록용 응답 DTO | Create |
| comment/infrastructure/out/CommentRepository.kt | JPA Repository | Modify - extend Custom |
| comment/infrastructure/out/CommentRepositoryCustom.kt | QueryDSL 인터페이스 | Create |
| comment/infrastructure/out/CommentRepositoryCustomImpl.kt | QueryDSL 구현 | Create |
| comment/application/CommentReadService.kt | 읽기 서비스 | Modify - 페이지네이션 로직 |
| comment/infrastructure/in/CommentController.kt | API 컨트롤러 | Modify - open-api, 2 endpoints |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO 네이밍 | BACKEND.md | ~Request, ~Response로 종료 |
| Enum 파일 분리 | BACKEND.md | Inner Class 금지, 별도 파일로 분리 |
| 커서 인코딩 | PostCursor.kt | sortType:sortValue:createdAt:id 형식, Base64 URL 인코딩 |
| Repository 패턴 | PostRepositoryCustomImpl.kt | QueryDSL + JPAQueryFactory |
| API 문서화 | BACKEND.md | Swagger 어노테이션, 한국어 description |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| API 경로 | /open-api/comments | 비회원도 조회 가능, Post API 패턴과 일관성 | /api/comments (인증 필요) |
| 커서 구조 | sortType:sortValue:createdAt:id | PostCursor 패턴 준수, 정렬 유연성 | createdAt+id만 사용 |
| 정렬 기본값 | LATEST | 일반적인 댓글 UX | LIKE |
| QueryDSL 사용 | Yes | 동적 정렬 조건, 기존 Post 패턴 준수 | JPA @Query |

## API Contracts

### GET /open-api/comments/posts/{postId}
- Headers: None required (open API)
- Request Params:
  - `postId`: UUID (path) - 게시물 ID
  - `cursor`: String (optional) - 이전 응답의 nextCursor
  - `size`: Int (default: 20, 1-100) - 페이지 크기
  - `sort`: CommentSortType (default: LATEST) - 정렬 기준
- Response: `ApiResponse<CursorPageResponse<CommentListResponse>>`
- Note: depth=0인 부모 댓글만 조회

### GET /open-api/comments/{commentId}/replies
- Headers: None required (open API)
- Request Params:
  - `commentId`: UUID (path) - 부모 댓글 ID
  - `cursor`: String (optional) - 이전 응답의 nextCursor
  - `size`: Int (default: 20, 1-100) - 페이지 크기
  - `sort`: CommentSortType (default: LATEST) - 정렬 기준
- Response: `ApiResponse<CursorPageResponse<CommentListResponse>>`
- Note: 특정 부모 댓글의 대댓글(depth=1)만 조회

## Data Models

### Comment Entity (Modified)
| Field | Type | Constraints |
|-------|------|-------------|
| likeCount | Long | default 0, 추천수 |
| replyCount | Long | default 0, 대댓글 수 |

### CommentSortType Enum
| Value | Description |
|-------|-------------|
| LATEST | 최신순 (createdAt DESC) |
| LIKE | 추천순 (likeCount DESC) |
| REPLY | 답글순 (replyCount DESC) |

## Implementation Todos

### Todo 1: CommentSortType enum 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 댓글 정렬 타입 enum 정의
- **Work**:
  - `comment/enums/CommentSortType.kt` 파일 생성
  - LATEST, LIKE, REPLY 값 정의
  - fromString() companion object 함수 추가 (PostSortType 패턴)
- **Convention Notes**: Enum은 별도 파일로 분리
- **Verification**: 컴파일 성공
- **Exit Criteria**: CommentSortType enum이 정상 생성되고 fromString() 동작
- **Status**: pending

### Todo 2: Comment 엔티티 수정
- **Priority**: 1
- **Dependencies**: none
- **Goal**: likeCount, replyCount 필드 추가
- **Work**:
  - `comment/entity/Comment.kt` 수정
  - `likeCount: Long = 0` 필드 추가 (@Column)
  - `replyCount: Long = 0` 필드 추가 (@Column)
- **Convention Notes**: EntityBase 상속 유지, @Column 어노테이션 사용
- **Verification**: 컴파일 성공, DDL auto 적용 확인
- **Exit Criteria**: Comment 엔티티에 두 필드가 추가됨
- **Status**: pending

### Todo 3: CommentCursor DTO 생성
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 커서 인코딩/디코딩 DTO 생성
- **Work**:
  - `comment/dto/CommentCursor.kt` 파일 생성
  - PostCursor 패턴 참고
  - sortValue, createdAt, id, sortType 필드
  - encode(), decode(), from() 함수 구현
- **Convention Notes**: Base64 URL 인코딩, 패딩 제거
- **Verification**: 단위 테스트로 encode/decode 검증
- **Exit Criteria**: CommentCursor가 정상적으로 인코딩/디코딩됨
- **Status**: pending

### Todo 4: CommentListResponse DTO 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 목록용 응답 DTO 생성
- **Work**:
  - `comment/dto/CommentListResponse.kt` 파일 생성
  - 기존 CommentResponse 필드 + likeCount, replyCount 추가
  - from(Comment) companion object 함수 추가
  - Swagger @Schema 어노테이션 추가
- **Convention Notes**: DTO는 ~Response로 종료, @field:Schema 사용
- **Verification**: 컴파일 성공
- **Exit Criteria**: CommentListResponse DTO가 정상 생성됨
- **Status**: pending

### Todo 5: CommentRepositoryCustom 인터페이스 생성
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 2
- **Goal**: QueryDSL 커스텀 쿼리 인터페이스 정의
- **Work**:
  - `comment/infrastructure/out/CommentRepositoryCustom.kt` 파일 생성
  - findParentCommentsWithConditions() 메서드 선언
  - findRepliesWithConditions() 메서드 선언
- **Convention Notes**: PostRepositoryCustom 패턴 준수
- **Verification**: 컴파일 성공
- **Exit Criteria**: 인터페이스가 정상 정의됨
- **Status**: pending

### Todo 6: CommentRepositoryCustomImpl 구현
- **Priority**: 3
- **Dependencies**: Todo 5
- **Goal**: QueryDSL 커스텀 쿼리 구현
- **Work**:
  - `comment/infrastructure/out/CommentRepositoryCustomImpl.kt` 파일 생성
  - JPAQueryFactory 주입
  - findParentCommentsWithConditions() 구현 (depth=0, postId 조건)
  - findRepliesWithConditions() 구현 (depth=1, parentId 조건)
  - 정렬 조건 동적 적용 (LATEST, LIKE, REPLY)
  - 커서 조건 적용 (where clause)
- **Convention Notes**: PostRepositoryCustomImpl 패턴 준수, QComment 사용
- **Verification**: 쿼리 로그 확인, 정렬/페이지네이션 동작 확인
- **Exit Criteria**: 두 메서드가 정상 동작, 정렬/커서 조건 적용됨
- **Status**: pending

### Todo 7: CommentRepository 수정
- **Priority**: 3
- **Dependencies**: Todo 5
- **Goal**: Custom 인터페이스 상속 추가
- **Work**:
  - `comment/infrastructure/out/CommentRepository.kt` 수정
  - CommentRepositoryCustom 상속 추가
- **Convention Notes**: JpaRepository, Custom 다중 상속
- **Verification**: 컴파일 성공
- **Exit Criteria**: CommentRepository가 Custom 메서드 사용 가능
- **Status**: pending

### Todo 8: CommentReadService 수정
- **Priority**: 4
- **Dependencies**: Todo 3, Todo 6, Todo 7
- **Goal**: 커서 기반 페이지네이션 로직 구현
- **Work**:
  - `comment/application/CommentReadService.kt` 수정
  - getParentComments() 메서드 추가 (부모 댓글 목록)
  - getReplies() 메서드 추가 (대댓글 목록)
  - PostListReadService.getPosts() 패턴 적용:
    - cursor decode
    - size + 1 조회
    - hasNext 판단
    - nextCursor 생성
    - CursorPageResponse 반환
- **Convention Notes**: @Transactional(readOnly = true), PostListReadService 패턴 준수
- **Verification**: 서비스 메서드 호출 테스트
- **Exit Criteria**: 두 메서드가 CursorPageResponse 정상 반환
- **Status**: pending

### Todo 9: CommentController 수정
- **Priority**: 5
- **Dependencies**: Todo 8
- **Goal**: open-api 경로로 변경, 2개 엔드포인트 구현
- **Work**:
  - `comment/infrastructure/in/CommentController.kt` 수정
  - 기존 `/api/comments` 유지 (댓글 작성 - 인증 필요)
  - 새로운 CommentReadController 클래스 추가 (또는 기존 getComments 수정)
  - `/open-api/comments/posts/{postId}` - 부모 댓글 목록
  - `/open-api/comments/{commentId}/replies` - 대댓글 목록
  - @RequestParam: cursor, size, sort
  - Swagger @Operation, @ApiResponses 추가
- **Convention Notes**: PostReadController 패턴 준수, @Min @Max validation
- **Verification**: Swagger UI에서 API 테스트
- **Exit Criteria**: 두 엔드포인트가 정상 동작, Swagger 문서화 완료
- **Status**: pending

## Verification Strategy
- 컴파일: `./gradlew :main-server:compileKotlin` 성공
- 애플리케이션 구동: `./gradlew :main-server:bootRun` 성공
- Swagger UI: `/swagger-ui.html`에서 API 테스트
- 기능 테스트:
  - 부모 댓글 목록 조회 (페이지네이션, 정렬)
  - 대댓글 목록 조회 (페이지네이션, 정렬)
  - 커서 값으로 다음 페이지 조회

## Progress Tracking
- Total Todos: 9
- Completed: 9
- Status: Execution complete

## Change Log
- 2026-02-05: Plan created
- 2026-02-05: All todos completed, build successful
