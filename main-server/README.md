## Google OAuth 인증

### 로그인 요청

```
GET /oauth2/authorization/google
```

브라우저에서 위 URL로 이동하면 Google 로그인 페이지로 리다이렉트됩니다.

### 인증 성공

Google 인증 성공 시 프론트엔드 URL로 리다이렉트되며, HttpOnly 쿠키로 토큰이 설정됩니다.

**리다이렉트 URL**: `http://localhost:3000/oauth/callback`

**프론트엔드 라우트**: `/oauth/callback`

**설정되는 쿠키**:

| 쿠키명 | 설명 | 만료 시간 | 속성 |
|--------|------|-----------|------|
| `accessToken` | API 인증용 토큰 | 1시간 | HttpOnly, Secure, SameSite=Lax |
| `refreshToken` | 토큰 갱신용 | 7일 | HttpOnly, Secure, SameSite=Lax |

**프론트엔드 처리 예시** (`/oauth/callback` 페이지):

```javascript
// React 예시
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function OAuthCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    // 쿠키는 자동으로 설정되어 있음 (HttpOnly라 JS에서 접근 불가)
    // 바로 메인 페이지로 이동
    navigate('/');
  }, [navigate]);

  return <div>로그인 처리 중...</div>;
}
```

### 인증 실패

인증 실패 시 에러 페이지로 리다이렉트됩니다.

**리다이렉트 URL**: `http://localhost:3000/oauth/error?error={code}&message={message}`

**프론트엔드 라우트**: `/oauth/error`

**쿼리 파라미터**:

| 파라미터 | 타입 | 설명 | 예시 |
|----------|------|------|------|
| `error` | number | 에러 코드 | `4003` |
| `message` | string | 에러 메시지 (URL 인코딩됨) | `OAuth+인증에+실패했습니다` |

**프론트엔드 처리 예시** (`/oauth/error` 페이지):

```javascript
// React 예시
import { useSearchParams, useNavigate } from 'react-router-dom';

export default function OAuthError() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const errorCode = searchParams.get('error');
  const errorMessage = searchParams.get('message');

  const getErrorDescription = (code) => {
    switch (code) {
      case '4001':
        return '지원하지 않는 로그인 방식입니다.';
      case '4002':
        return '이메일 정보를 가져올 수 없습니다. Google 계정 설정을 확인해주세요.';
      case '4003':
        return '로그인에 실패했습니다. 다시 시도해주세요.';
      case '4004':
        return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
      default:
        return errorMessage || '알 수 없는 오류가 발생했습니다.';
    }
  };

  return (
    <div>
      <h1>로그인 실패</h1>
      <p>{getErrorDescription(errorCode)}</p>
      <button onClick={() => navigate('/login')}>
        다시 로그인하기
      </button>
    </div>
  );
}
```

### API 요청 시 인증

쿠키가 자동으로 포함되므로 별도 설정 불필요. CORS 환경에서는 `credentials: 'include'` 필요.

```javascript
// 브라우저 fetch
fetch('/api/data', {
  credentials: 'include'
})

// axios
axios.get('/api/data', {
  withCredentials: true
})
```

또는 Authorization 헤더 사용:

```
Authorization: Bearer {accessToken}
```

---

## 에러 코드

### OAuthStatus

| HTTP Status | Custom Code | 설명 |
|-------------|-------------|------|
| 400 | 4001 | 지원하지 않는 OAuth Provider입니다 |
| 400 | 4002 | OAuth 응답에서 이메일을 찾을 수 없습니다 |
| 401 | 4003 | OAuth 인증에 실패했습니다 |
| 500 | 4004 | 사용자 정보를 불러오는데 실패했습니다 |

**위치**: `security/oauth/status/OAuthStatus.kt`

### BlogStatus

| HTTP Status | Custom Code | 설명 |
|-------------|-------------|------|
| 404 | 3001 | Blog not found |

**위치**: `blog/BlogStatus.kt`

---

## ArticleLink 및 BatchLog 시스템

### 개요
크롤링한 게시글 링크를 관리하고 배치 작업 실행 내역을 추적하는 시스템

블로그와 게시글 링크를 1:N 관계로 정규화하여 관리합니다.

### Entity

#### Blog
- **위치**: `com.techtaurant.mainserver.blog.entity.Blog`
- **테이블**: `blogs`
- **특징**:
  - Soft Delete 적용: 삭제 시 실제 DB에서 제거되지 않고 `deleted_at`에 현재 시간 기록
  - Full Text Search 지원: `name`, `displayName` 컬럼에 pg_trgm 기반 GIN 인덱스
- **필드**:
  - `id`: UUID (Primary Key)
  - `name`: 블로그 식별자 (Unique, 예: "toss", "kakao")
  - `displayName`: 표시용 이름
  - `iconUrl`: 블로그 아이콘 URL
  - `baseUrl`: 블로그 기본 URL
  - `createdAt`: 생성일시
  - `updatedAt`: 수정일시
  - `deletedAt`: 삭제일시 (Soft Delete용)
- **어노테이션**:
  - `@SQLDelete`: DELETE 쿼리를 UPDATE로 변환하여 Soft Delete 구현
  - `@SQLRestriction`: SELECT 시 `deleted_at IS NULL` 조건 자동 적용

#### ArticleLink
- **위치**: `com.techtaurant.mainserver.articlelink.entity.ArticleLink`
- **테이블**: `article_links`
- **필드**:
  - `id`: UUID (Primary Key)
  - `blog`: Blog 엔티티 (ManyToOne, FK)
  - `url`: 게시글 URL
  - `title`: 게시글 제목
  - `type`: 크롤링 타입 (PAGE_BASED, NEXT_BUTTON_BASED)
  - `createdAt`: 생성일시
  - `updatedAt`: 수정일시
  - `deletedAt`: 삭제일시 (소프트 삭제)

#### BatchLog
- **위치**: `com.techtaurant.mainserver.batchlog.entity.BatchLog`
- **테이블**: `batch_log`
- **필드**:
  - `id`: UUID (Primary Key)
  - `batchName`: 배치 작업 이름
  - `status`: 배치 상태 (RUNNING, SUCCESS, FAILED)
  - `startedAt`: 시작 시간
  - `finishedAt`: 종료 시간
  - `errorMessage`: 에러 메시지
  - `screenshotBase64`: 실패시 스크린샷 (Base64)
  - `createdAt`: 생성일시
  - `updatedAt`: 수정일시

### Repository

#### BlogRepository
- **위치**: `com.techtaurant.mainserver.blog.infrastructure.out.BlogRepository`
- **주요 메서드**:
  - `findByNameAndDeletedAtIsNull(name: String)`: 블로그명으로 조회 (삭제되지 않은 것만)
  - `findByDeletedAtIsNull()`: 모든 블로그 조회 (삭제되지 않은 것만)

#### ArticleLinkRepository
- **위치**: `com.techtaurant.mainserver.articlelink.infrastructure.out.ArticleLinkRepository`
- **주요 메서드**:
  - `findByBlogAndDeletedAtIsNull(blog: Blog)`: 블로그로 조회 (삭제되지 않은 것만)
  - `findByTypeAndDeletedAtIsNull(type: ArticleLinkType)`: 타입으로 조회 (삭제되지 않은 것만)

#### BatchLogRepository
- **위치**: `com.techtaurant.mainserver.batchlog.infrastructure.out.BatchLogRepository`
- **주요 메서드**:
  - `findByBatchName(batchName: String)`: 배치명으로 조회
  - `findByStatus(status: BatchStatus)`: 상태로 조회

### Service

#### BlogService
- **위치**: `com.techtaurant.mainserver.blog.service.BlogService`
- **기능**: 블로그 CRUD 관리
- **메서드**:
  - `createBlog(request: BlogCreateRequest): BlogResponse` - 블로그 생성
  - `updateBlog(id: UUID, request: BlogUpdateRequest): BlogResponse` - 블로그 수정
  - `deleteBlog(id: UUID)` - 블로그 삭제
  - `getBlogById(id: UUID): BlogResponse` - 블로그 조회
  - `getAllBlogs(): List<BlogResponse>` - 전체 블로그 목록
  - `searchBlogByName(name: String): BlogResponse?` - 블로그 검색

#### CrawlerApiService
- **위치**: `com.techtaurant.mainserver.articlelink.service.CrawlerApiService`
- **기능**: Python 크롤러 API 호출
- **메서드**:
  - `validateCrawlingPage(request: PageBaseLinkCrawlingRequest): Mono<Boolean>`
    - 크롤링 페이지 유효성 검증
    - 엔드포인트: POST `/api/v1/page-base-link-crawling/validations`

#### ArticleLinkService
- **위치**: `com.techtaurant.mainserver.articlelink.service.ArticleLinkService`
- **기능**: 크롤링 유효성 검증 후 링크 저장
- **메서드**:
  - `validateAndSave(request: ValidationRequest): Boolean`
    - 크롤러 API로 유효성 검증
    - 검증 성공시 ArticleLink를 article_links 테이블에 저장
    - 트랜잭션 처리
    - **주의**: 블로그는 사전에 등록되어 있어야 함

### API

#### Admin API (ROLE_ADMIN 필요)

##### POST /admin/blogs
블로그 등록

**Request Body**:
```json
{
  "name": "toss",
  "displayName": "토스 기술 블로그",
  "iconUrl": "https://example.com/icon.png",
  "baseUrl": "https://toss.tech/"
}
```

**Response**:
```json
{
  "status": 200,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "toss",
    "displayName": "토스 기술 블로그",
    "iconUrl": "https://example.com/icon.png",
    "baseUrl": "https://toss.tech/"
  },
  "message": "OK"
}
```

##### PUT /admin/blogs/{id}
블로그 수정

**Request Body**:
```json
{
  "displayName": "토스 기술 블로그 (수정)",
  "iconUrl": "https://example.com/new-icon.png",
  "baseUrl": "https://toss.tech/"
}
```

##### DELETE /admin/blogs/{id}
블로그 삭제

#### Blog API (인증 필요)

##### GET /api/v1/blogs
전체 블로그 목록 조회

**Response**:
```json
{
  "status": 200,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "toss",
      "displayName": "토스 기술 블로그",
      "iconUrl": "https://example.com/icon.png",
      "baseUrl": "https://toss.tech/"
    }
  ],
  "message": "OK"
}
```

##### GET /api/v1/blogs/{id}
특정 블로그 조회

##### GET /api/v1/blogs/search?name={name}
블로그 검색

#### ArticleLink API

##### POST /api/v1/article-links/validate-and-save
크롤링 설정을 검증하고 유효한 경우 DB에 저장

**Request Body**:
```json
{
  "blogId": "550e8400-e29b-41d4-a716-446655440000",
  "baseUrl": "https://toss.tech/",
  "startPage": 1,
  "articlePattern": "/article/",
  "titleSelector": "span.e1sck7qg7",
  "type": "PAGE_BASED"
}
```

**Response**:
```json
{
  "status": 200,
  "data": true,
  "message": "OK"
}
```

### Configuration

#### WebClientConfig
- **위치**: `com.techtaurant.mainserver.common.config.WebClientConfig`
- **Bean**: `crawlerWebClient`
- **설정값**:
  - `crawler.api.base-url`: 크롤러 API 기본 URL (기본값: http://localhost:8000)
  - `crawler.api.timeout`: 요청 타임아웃 (기본값: 30000ms)

### 환경 변수
`.env` 파일에 추가:
```properties
CRAWLER_API_BASE_URL=http://localhost:8000
CRAWLER_API_TIMEOUT=30000
```

### Migration 파일
- `V1__create_user.sql`: user 테이블 생성
- `V2__create_blogs.sql`: blogs 테이블 생성
- `V3__create_article_links.sql`: article_links 테이블 생성 (blog_id FK 포함)
- `V4__create_batch_log.sql`: batch_log 테이블 생성
- `V5__create_fulltext_index_for_blogs.sql`: blogs 테이블 Full Text Search 인덱스 생성

---

## Full Text Search (블로그 검색)

### 개요
PostgreSQL의 pg_trgm extension을 사용한 한국어 블로그 검색 기능

### 특징
- **N-Gram 기반 검색**: trigram (3-gram) 유사도 검색 지원
- **한국어 2글자 검색 지원**: similarity threshold 조정으로 짧은 한국어 검색어도 지원
- **오타 허용**: 유사도 기반 검색으로 오타가 있어도 검색 가능
- **인덱스 최적화**: GIN 인덱스로 대용량 데이터에서도 빠른 검색

### 인덱스 구성
1. **idx_blogs_name_trgm**: `name` 컬럼 trigram 인덱스
2. **idx_blogs_display_name_trgm**: `display_name` 컬럼 trigram 인덱스
3. **idx_blogs_combined_trgm**: `name + display_name` 복합 검색 인덱스

### 검색 방법

#### 1. ILIKE를 사용한 부분 일치 검색 (정확한 부분 문자열)
```sql
SELECT * FROM blogs
WHERE name ILIKE '%검색어%'
  AND deleted_at IS NULL;
```

**특징**:
- 정확한 부분 문자열 매칭
- 대소문자 구분 없음
- 가장 직관적이고 이해하기 쉬움

#### 2. similarity 함수를 사용한 유사도 검색 (오타 허용)
```sql
SELECT *, similarity(name, '검색어') as sim
FROM blogs
WHERE name % '검색어'
  AND deleted_at IS NULL
ORDER BY sim DESC;
```

**특징**:
- 유사도 점수(0~1) 반환
- `%` 연산자: 기본 threshold(0.3) 이상인 것만 반환
- 오타가 있어도 검색 가능
- 검색 결과를 유사도 순으로 정렬 가능

#### 3. word_similarity를 사용한 단어 단위 유사도 검색
```sql
SELECT *, word_similarity('검색어', name) as sim
FROM blogs
WHERE '검색어' <% name
  AND deleted_at IS NULL
ORDER BY sim DESC;
```

**특징**:
- 단어 단위로 유사도 계산
- `<%` 연산자 사용
- 긴 문자열에서 짧은 검색어를 찾을 때 유리

#### 4. 복합 검색 (name + displayName 통합)
```sql
SELECT * FROM blogs
WHERE (name || ' ' || COALESCE(display_name, '')) % '검색어'
  AND deleted_at IS NULL;
```

**특징**:
- name과 displayName을 하나로 합쳐서 검색
- 어느 필드에 있든 검색 가능
- idx_blogs_combined_trgm 인덱스 활용

### Native Query 예시 (Spring Data JPA)

```kotlin
interface BlogRepository : JpaRepository<Blog, UUID> {

    // ILIKE 검색
    @Query(
        """
        SELECT b FROM Blog b
        WHERE (b.name LIKE %:keyword% OR b.displayName LIKE %:keyword%)
          AND b.deletedAt IS NULL
        """
    )
    fun searchByKeyword(@Param("keyword") keyword: String): List<Blog>

    // 유사도 검색 (Native Query)
    @Query(
        value = """
        SELECT *, similarity(name, :keyword) as sim
        FROM blogs
        WHERE name % :keyword
          AND deleted_at IS NULL
        ORDER BY sim DESC
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchBySimilarity(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int = 10
    ): List<Blog>

    // 복합 검색
    @Query(
        value = """
        SELECT * FROM blogs
        WHERE (name || ' ' || COALESCE(display_name, '')) % :keyword
          AND deleted_at IS NULL
        ORDER BY similarity((name || ' ' || COALESCE(display_name, '')), :keyword) DESC
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchCombined(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int = 10
    ): List<Blog>
}
```

### Similarity Threshold 조정

기본 threshold는 0.3이며, 0.1~0.3 사이로 설정하면 한국어 2글자도 검색 가능합니다.

#### 세션별 설정
```sql
SET pg_trgm.similarity_threshold = 0.2;
```

#### 전역 설정 (application.yml)
```yaml
spring:
  datasource:
    hikari:
      connection-init-sql: "SET pg_trgm.similarity_threshold = 0.2"
```

### 성능 최적화 팁

1. **EXPLAIN ANALYZE로 쿼리 분석**
```sql
EXPLAIN ANALYZE
SELECT * FROM blogs WHERE name % '검색어';
```

2. **인덱스 사용 확인**
   - GIN 인덱스가 사용되는지 확인 (`Index Scan using idx_blogs_name_trgm`)
   - 인덱스가 사용되지 않으면 threshold를 조정하거나 VACUUM 실행

3. **적절한 LIMIT 사용**
   - 검색 결과가 많을 경우 LIMIT으로 제한
   - 유사도 순으로 정렬 후 상위 N개만 반환

4. **정기적인 VACUUM**
```sql
VACUUM ANALYZE blogs;
```

### 주의사항

1. **pg_trgm extension이 활성화되어 있어야 함**
   - V5 migration 실행 시 자동으로 활성화됨
   - 수동 확인: `SELECT * FROM pg_extension WHERE extname = 'pg_trgm';`

2. **인덱스 재생성이 필요한 경우**
```sql
REINDEX INDEX idx_blogs_name_trgm;
REINDEX INDEX idx_blogs_display_name_trgm;
REINDEX INDEX idx_blogs_combined_trgm;
```

3. **매우 짧은 검색어 (1글자)**
   - trigram은 최소 2글자부터 효과적
   - 1글자 검색은 ILIKE 사용 권장

