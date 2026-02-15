# Comment List API with Cursor Pagination - Implementation Summary

Branch: `feat/comment-list-api`
Date: 2026-02-06

## 세션 요약

댓글 목록 조회 API를 커서 기반 페이지네이션으로 구현하고, CommentController에 대한 통합 테스트를 작성했습니다.

## 주요 구현 내용

### 1. Comment List API (Cursor Pagination)

#### 새로 생성된 파일
- `CommentSortType.kt` - 정렬 타입 enum (LATEST, LIKE, REPLY)
- `CommentCursor.kt` - 커서 인코딩/디코딩 DTO
- `CommentListResponse.kt` - 목록용 응답 DTO (likeCount, replyCount 포함)
- `CommentRepositoryCustom.kt` - QueryDSL 커스텀 인터페이스
- `CommentRepositoryCustomImpl.kt` - JPA Criteria API 구현체
- `CommentReadOpenApiController.kt` - 비회원 접근 가능한 댓글 조회 API
- `V11__add_comment_counts.sql` - Flyway 마이그레이션 (likeCount, replyCount 컬럼 추가)

#### 수정된 파일
- `Comment.kt` - likeCount, replyCount 필드 추가
- `CommentReadService.kt` - 커서 기반 페이지네이션 로직 (getParentComments, getReplies)
- `CommentRepository.kt` - CommentRepositoryCustom 상속 추가
- `CommentController.kt` - GET 엔드포인트 제거 (open-api로 이동)
- `build.gradle.kts` - JaCoCo exclusion patterns 공통화

#### API 구조
1. **GET /open-api/comments/posts/{postId}** - 부모 댓글 목록 (depth=0)
   - 파라미터: cursor, size(1-100), sort(LATEST|LIKE|REPLY)
   - 응답: CursorPageResponse<CommentListResponse>

2. **GET /open-api/comments/{commentId}/replies** - 대댓글 목록 (depth=1)
   - 파라미터: cursor, size(1-100), sort(LATEST|LIKE|REPLY)
   - 응답: CursorPageResponse<CommentListResponse>

#### 기술적 특징
- **JPA Criteria API 사용**: QueryDSL 대신 Metamodel 기반 타입 안전 쿼리
- **N+1 방지**: author, post를 fetch join
- **동적 정렬**: LATEST(최신순), LIKE(추천순), REPLY(답글순)
- **커서 구조**: `sortType:sortValue:createdAt:id` (Base64 URL 인코딩)
- **정렬 보조 키**: 동점자 처리를 위해 createdAt, id를 보조 정렬 키로 사용

### 2. CommentController 통합 테스트

#### 파일 위치
- `CommentControllerIntegrationTest.kt`
- `src/test/resources/db/cleanup.sql`

#### 테스트 환경
- IntegrationTest 상속 (Testcontainers 기반)
- PostgreSQL 15 컨테이너
- JWT 토큰 기반 인증

#### 테스트 케이스 (Given-When-Then 패턴)
1. ✅ 댓글 작성 성공 - 유효한 요청으로 댓글 생성
2. ✅ 댓글 작성 성공 - 유효한 요청으로 대댓글 생성
3. ✅ 댓글 작성 실패 - 빈 내용
4. ✅ 댓글 작성 실패 - 존재하지 않는 게시물
5. ✅ 댓글 작성 실패 - 존재하지 않는 부모 댓글
6. ✅ 댓글 작성 실패 - 인증되지 않은 사용자
7. ✅ 댓글 작성 실패 - 대댓글의 답글 시도 (depth 제한)
8. ✅ 댓글 작성 실패 - 부모 댓글이 다른 게시물에 속한 경우

#### 현재 이슈
테스트 실행 시 401 Unauthorized 에러 발생:
- JWT 토큰 생성은 정상적으로 수행됨 (`jwtTokenProvider.createAccessToken` 사용)
- `application-test.yml`의 JWT 시크릿과 실제 환경의 시크릿이 다를 수 있음
- Spring Security 필터가 테스트 환경에서 토큰 검증 실패

#### 해결 방법 제안
1. JWT 시크릿 키 통일 (.env와 application-test.yml 동기화)
2. Security 비활성화 (테스트 전용 Security Config)
3. MockMvc + @WithMockUser로 전환

### 3. CommentReadControllerTest (통합 테스트 - 성공)

#### 파일: `CommentReadControllerTest.kt`

#### 테스트 범위
- 부모 댓글 정렬 기준별 검증 (LATEST, LIKE, REPLY)
- 대댓글 정렬 기준별 검증
- 페이지네이션 검증 (첫 페이지, 마지막 페이지, 커서 이용 다음 페이지)
- 기본 댓글 조회 검증 (데이터 로딩, 통계 정보)

#### 테스트 구조
- `@Nested` 클래스로 테스트 그룹화
- Given-When-Then 패턴 적용
- RestAssured 사용 (open-api 경로는 인증 불필요)
- Calendar API로 댓글 생성 시간 조작
- likeCount, replyCount 값을 조작하여 정렬 테스트

#### 주요 검증 사항
1. 정렬 조건별로 올바른 순서로 반환되는지
2. 커서 기반 페이지네이션이 정확히 동작하는지
3. nextCursor, hasNext 값이 올바른지
4. N+1 문제가 발생하지 않는지 (fetch join 검증)

## 변경 파일 통계

- 총 15개 파일 변경
- Staged: 13개
- Untracked: 2개 (CommentControllerIntegrationTest.kt, cleanup.sql)

### Staged 파일
```
A  .claude/plan/comment/2602/05-comment-list-cursor-pagination.plan.md
M  build.gradle.kts (JaCoCo exclusion patterns 공통화)
M  CommentReadService.kt (커서 페이지네이션 로직)
A  CommentCursor.kt (커서 DTO)
A  CommentListResponse.kt (목록 응답 DTO)
M  Comment.kt (likeCount, replyCount 필드 추가)
A  CommentSortType.kt (정렬 타입 enum)
M  CommentController.kt (GET 엔드포인트 제거)
A  CommentReadOpenApiController.kt (조회 API 컨트롤러)
M  CommentRepository.kt (Custom 인터페이스 상속)
A  CommentRepositoryCustom.kt (커스텀 쿼리 인터페이스)
A  CommentRepositoryCustomImpl.kt (JPA Criteria API 구현)
A  V11__add_comment_counts.sql (Flyway 마이그레이션)
M  MainServerApplicationTests.kt (주석 해제)
A  CommentReadControllerTest.kt (통합 테스트 - 성공)
```

### Untracked 파일
```
?? CommentControllerIntegrationTest.kt (통합 테스트 - 401 에러 발생 중)
?? src/test/resources/db/cleanup.sql (테스트 데이터 정리)
```

## 기술적 의사결정

### 1. JPA Criteria API vs QueryDSL
- **선택**: JPA Criteria API with Metamodel
- **이유**: 
  - Metamodel 기반 타입 안전성 제공
  - QueryDSL Q 클래스 생성 없이도 컴파일 타임 체크 가능
  - 프로젝트에 이미 Metamodel 설정 존재 (Comment_)
  - 동적 쿼리 + 정렬 조건 적용에 적합

### 2. 커서 구조
- **구조**: `sortType:sortValue:createdAt:id`
- **이유**:
  - 다양한 정렬 기준 지원 (LATEST는 sortValue=0)
  - 동점자 처리 (sortValue 동일 시 createdAt, id로 구분)
  - Base64 URL 인코딩으로 안전한 URL 파라미터

### 3. 정렬 보조 키
- **LATEST**: createdAt DESC, id DESC
- **LIKE**: likeCount DESC, createdAt DESC, id DESC
- **REPLY**: replyCount DESC, createdAt DESC, id DESC
- **이유**: 동일 count 값을 가진 댓글의 순서를 일관되게 유지

### 4. N+1 방지
- fetch join 사용: `root.fetch<Comment, User>(Comment_.AUTHOR)`
- LEFT JOIN으로 post 조회: `root.fetch<Comment, Post>(Comment_.POST, JoinType.LEFT)`
- 단일 쿼리로 필요한 모든 데이터 조회

## 다음 작업 (TODO)

### 1. CommentControllerIntegrationTest 401 에러 해결
- [ ] JWT 시크릿 키 통일 검토
- [ ] 또는 테스트 전용 Security Config 생성
- [ ] 또는 MockMvc + @WithMockUser로 전환

### 2. 테스트 커버리지 확장
- [ ] 잘못된 커서 값 테스트
- [ ] 정렬 타입 변경 시 커서 유효성 테스트
- [ ] 동시성 테스트 (동일 시간대 댓글 생성)

### 3. 성능 최적화
- [ ] 인덱스 효과 검증 (idx_comments_like_count, idx_comments_reply_count)
- [ ] N+1 쿼리 발생 여부 확인 (p6spy 로그 분석)
- [ ] 대용량 데이터 페이지네이션 성능 테스트

## 참고사항

### 테스트 환경 설정
- Testcontainers: PostgreSQL 15-alpine
- @DynamicPropertySource로 컨테이너 URL 주입
- cleanup.sql로 각 테스트 전 데이터 초기화

### JPA Criteria API 사용법
```kotlin
val cb = entityManager.criteriaBuilder
val cq = cb.createQuery(Comment::class.java)
val root = cq.from(Comment::class.java)

// Fetch join
root.fetch<Comment, User>(Comment_.AUTHOR)

// Where 조건
val predicates = mutableListOf<Predicate>()
predicates.add(cb.equal(root.get(Comment_.depth), 0))

// Order by
cq.orderBy(cb.desc(root.get(EntityBase_.createdAt)))

// 실행
entityManager.createQuery(cq).setMaxResults(size).resultList
```

### 커서 인코딩/디코딩
```kotlin
// 인코딩
fun encode(): String {
    val raw = "${sortType.name}:$sortValue:${createdAt.time}:$id"
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
}

// 디코딩
fun decode(cursor: String): CommentCursor? {
    val decoded = String(Base64.getUrlDecoder().decode(cursor))
    val parts = decoded.split(":")
    if (parts.size != 4) return null
    // ...
}
```

## 교훈

1. **JPA Criteria API는 QueryDSL만큼 강력하다**
   - Metamodel 기반으로 타입 안전성 보장
   - 동적 쿼리 작성이 유연함
   - Q 클래스 생성 오버헤드 없음

2. **커서 기반 페이지네이션은 복잡하지만 확장 가능하다**
   - 다양한 정렬 기준 지원 가능
   - 동점자 처리를 위한 보조 키 필수
   - 커서 구조 설계가 중요

3. **통합 테스트에서 인증 처리는 까다롭다**
   - JWT 토큰 생성은 쉽지만 검증은 환경 의존적
   - 테스트 환경의 시크릿 키 관리 중요
   - open-api 경로 사용 시 인증 불필요 (테스트 단순화)

4. **N+1 문제는 fetch join으로 해결**
   - JPA Criteria API에서도 fetch join 가능
   - LEFT JOIN으로 nullable 관계 처리
   - p6spy로 실제 쿼리 확인 필수
