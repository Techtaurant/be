# 게시물 목록 조회 API 확장: 사용자 프로필, 썸네일, 읽음 여부 추가

## Business Goal
게시물 목록 조회 시 사용자 경험을 향상시키기 위해 작성자 프로필 이미지, 게시물 썸네일, 읽음 여부 정보를 추가로 제공합니다. 이를 통해 사용자는 게시물 목록에서 더 풍부한 정보를 한눈에 파악할 수 있으며, 읽지 않은 게시물을 쉽게 식별할 수 있습니다.

## Scope
- **In Scope**:
  - post_pictures 테이블 생성 및 PostPicture 엔티티 구현
  - 썸네일 이미지 관리 (is_thumbnail 플래그)
  - 기본 썸네일 이미지 설정 (정적 리소스)
  - 작성자 프로필 이미지 URL 응답에 포함
  - PostViewLog 기반 읽음 여부 판단
  - Repository Fetch Join 최적화 (N+1 방지)
  - DTO 확장 (PostListItemResponse)
  - Security Context에서 현재 사용자 정보 추출
  - 비회원도 목록 조회 가능 (isRead는 항상 false)

- **Out of Scope**:
  - 게시물 사진 업로드 API 구현
  - OAuth 프로필 이미지 저장 로직 수정
  - 게시물 읽음 처리(PostViewLog 생성) API

## Codebase Analysis Summary
- **Tech Stack**: Spring Boot 3.5.7, Kotlin, JPA, QueryDSL, PostgreSQL, Flyway
- **현재 구조**:
  - Post 엔티티는 author(User)와 ManyToOne 관계
  - User 엔티티는 profileImageUrl 필드 보유
  - PostViewLog 엔티티로 조회 이벤트 기록 (user_id nullable)
  - PostListReadService에서 커서 기반 페이지네이션 구현
  - PostRepositoryCustomImpl에서 QueryDSL 기반 동적 쿼리
- **Conventions**:
  - DTO 네이밍: ~Request, ~Response
  - Entity UUID: uuid.v7() 사용
  - Enum은 별도 파일로 분리, VARCHAR로 저장
  - 주석: 한국어 서술형, JavaDoc 스타일
  - Exception: ApiException + Status enum 활용

### Relevant Files
| File | Role | Action |
|------|------|--------|
| src/main/resources/db/migration/V10__create_post_pictures.sql | DB 스키마 | Create |
| src/main/kotlin/com/techtaurant/mainserver/post/entity/PostPicture.kt | 게시물 사진 엔티티 | Create |
| src/main/kotlin/com/techtaurant/mainserver/post/entity/Post.kt | 게시물 엔티티 | Modify (pictures 관계 추가) |
| src/main/kotlin/com/techtaurant/mainserver/post/dto/PostListItemResponse.kt | 목록 응답 DTO | Modify (필드 추가) |
| src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepositoryCustomImpl.kt | 커스텀 Repository | Modify (Fetch Join 추가) |
| src/main/kotlin/com/techtaurant/mainserver/post/application/PostListReadService.kt | 목록 조회 서비스 | Modify (읽음 여부 로직) |
| src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostController.kt | Controller | Modify (인증 정보 전달) |
| src/main/resources/application.yml | 설정 파일 | Modify (기본 썸네일 URL) |
| src/main/resources/static/images/default-thumbnail.png | 기본 이미지 | Create |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Entity 관계 | Post-Tag 패턴 | OneToMany with Cascade.PERSIST, MERGE |
| DTO 변환 | PostListItemResponse | companion object fun from(entity) 패턴 |
| 주석 스타일 | CODE_PRINCIPLES.md | 한국어 서술형, 번호 없이 자연스러운 줄글 |
| Boolean 네이밍 | CODE_PRINCIPLES.md | is, has, can, should 접두사 사용 |
| Repository 확장 | PostRepositoryCustomImpl | QueryDSL 활용, interface + Impl 분리 |
| Exception 처리 | PostStatus enum | ApiException(PostStatus.XXX) 패턴 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 사진 저장 방식 | post_pictures 테이블 (OneToMany) | 여러 사진 지원, is_thumbnail 플래그로 명확한 구분, 확장성 확보 | Post.thumbnailUrl 단일 컬럼 (확장성 부족) |
| 썸네일 선택 로직 | is_thumbnail=true 우선, 없으면 display_order 낮은 순 | 명시적 지정 우선, fallback 로직으로 유연성 확보 | display_order만 사용 (의도 불명확) |
| 기본 이미지 | 정적 리소스 경로 (application.yml 설정) | 설정으로 관리, 추후 CDN 전환 용이, 환경별 다른 이미지 가능 | 하드코딩 (유지보수 어려움) |
| 프로필 정보 범위 | 이름 + 프로필 이미지만 | 응답 크기 최소화, GDPR 고려, 필요한 정보만 제공 | UserResponse 전체 (불필요한 데이터 노출) |
| 읽음 여부 판단 | PostViewLog 조인 | 기존 인프라 재활용, 추가 테이블 불필요, 일관성 유지 | post_read_status 별도 테이블 (중복 구조) |
| 성능 최적화 | Fetch Join (author, pictures, viewLog) | N+1 쿼리 방지, 단일 쿼리로 필요 데이터 전부 조회 | Lazy Loading (N+1 발생) |
| 인증 처리 | SecurityContextHolder.getContext() | Spring Security 표준 방식, 비회원은 null 처리 | @AuthenticationPrincipal (비회원 지원 복잡) |
| DTO 필드 타입 | authorProfileImageUrl: String, thumbnailUrl: String, isRead: Boolean | nullable 대신 기본값 사용 (빈 문자열, false) | nullable (null 체크 부담) |

## API Contracts

### GET /open-api/posts
기존 파라미터 유지, 응답 스키마만 확장

**Request Parameters**:
- cursor: String? (optional) - 이전 응답의 nextCursor
- size: Int (default: 20, min: 1, max: 100) - 페이지 크기
- period: PostPeriod (default: ALL) - 기간 필터
- sort: PostSortType (default: LATEST) - 정렬 기준

**Response**: `ApiResponse<CursorPageResponse<PostListItemResponse>>`

**PostListItemResponse 변경사항**:
```kotlin
data class PostListItemResponse(
    val id: UUID,
    val title: String,
    val authorName: String,
    val authorProfileImageUrl: String,  // 신규 추가
    val thumbnailUrl: String,           // 신규 추가
    val isRead: Boolean,                // 신규 추가
    val tags: List<PostListTagResponse>,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val createdAt: Date,
)
```

**Authorization**: Optional (비회원도 조회 가능, 로그인 시 isRead 판단)

**Note**:
- authorProfileImageUrl이 빈 문자열이면 클라이언트에서 기본 아바타 표시
- thumbnailUrl이 빈 문자열이면 서버 기본 이미지 경로 반환
- 비회원은 isRead 항상 false

## Data Models

### post_pictures (신규)
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK |
| post_id | UUID | FK (posts.id), NOT NULL, ON DELETE CASCADE |
| picture_url | VARCHAR(500) | NOT NULL |
| is_thumbnail | BOOLEAN | NOT NULL, DEFAULT false |
| display_order | INTEGER | NOT NULL, DEFAULT 0 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

**Indexes**:
- idx_post_pictures_post_id (post_id)
- idx_post_pictures_thumbnail (post_id, is_thumbnail) - 썸네일 조회 최적화

### PostPicture Entity (신규)
```kotlin
@Entity
@Table(name = "post_pictures")
class PostPicture(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    var post: Post,

    @Column(name = "picture_url", nullable = false, length = 500)
    var pictureUrl: String,

    @Column(name = "is_thumbnail", nullable = false)
    var isThumbnail: Boolean = false,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
) : EntityBase()
```

### Post Entity 수정
```kotlin
@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
var pictures: MutableSet<PostPicture> = mutableSetOf()
```

## Implementation Todos

### Todo 1: DB 마이그레이션 파일 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: post_pictures 테이블을 PostgreSQL DB에 추가
- **Work**:
  - `src/main/resources/db/migration/V10__create_post_pictures.sql` 파일 생성
  - post_pictures 테이블 정의 (id, post_id, picture_url, is_thumbnail, display_order, created_at, updated_at)
  - idx_post_pictures_post_id 인덱스 생성
  - idx_post_pictures_thumbnail (post_id, is_thumbnail) 복합 인덱스 생성
  - post_id에 FK 제약조건 추가 (ON DELETE CASCADE)
- **Convention Notes**:
  - Flyway 네이밍: V{버전}__{설명}.sql (V10__create_post_pictures.sql)
  - 컬럼명: snake_case 사용
  - UUID 타입은 PostgreSQL UUID 사용
  - TIMESTAMP는 DEFAULT CURRENT_TIMESTAMP
- **Verification**:
  - Flyway 마이그레이션 실행 확인 (`./gradlew flywayMigrate`)
  - flyway_schema_history 테이블에 V10 기록 확인
  - post_pictures 테이블 생성 확인
- **Exit Criteria**: Flyway 마이그레이션 성공, 테이블 및 인덱스 정상 생성
- **Status**: pending

### Todo 2: PostPicture 엔티티 생성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: post_pictures 테이블과 매핑되는 JPA 엔티티 생성
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/entity/PostPicture.kt` 파일 생성
  - EntityBase 상속
  - post (ManyToOne), pictureUrl, isThumbnail, displayOrder 필드 정의
  - 생성자 파라미터로 필수 필드 정의, 옵션 필드는 기본값 설정
  - JavaDoc 주석 추가 (한국어 서술형)
- **Convention Notes**:
  - Entity 클래스명: 단수형 (PostPicture)
  - 필드명: camelCase (isThumbnail, displayOrder)
  - @Column annotation에 name, nullable 명시
  - 주석: 각 필드의 비즈니스 의미 설명
- **Verification**:
  - 컴파일 성공
  - 애플리케이션 시작 시 Hibernate DDL 검증 통과
- **Exit Criteria**: PostPicture 엔티티 생성 완료, 컴파일 및 런타임 에러 없음
- **Status**: pending

### Todo 3: Post 엔티티에 pictures 관계 추가
- **Priority**: 2
- **Dependencies**: Todo 2
- **Goal**: Post와 PostPicture 간 OneToMany 관계 설정
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/entity/Post.kt` 수정
  - pictures 필드 추가: `@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])`
  - MutableSet<PostPicture> 타입 사용
  - 주석 추가: "게시물에 첨부된 사진 목록"
- **Convention Notes**:
  - 기존 tags 필드와 동일한 패턴 사용 (OneToMany, MutableSet, Cascade)
  - FetchType.LAZY 유지 (N+1 방지는 Repository에서 Fetch Join으로 해결)
- **Verification**:
  - 컴파일 성공
  - Post 엔티티 조회 시 pictures lazy loading 동작 확인
- **Exit Criteria**: Post-PostPicture 관계 정상 동작
- **Status**: pending

### Todo 4: 기본 썸네일 이미지 추가 및 설정
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 게시물 사진이 없을 때 사용할 기본 이미지 설정
- **Work**:
  - `src/main/resources/static/images/` 디렉토리 생성
  - `default-thumbnail.png` 파일 배치 (placeholder 이미지)
  - `src/main/resources/application.yml` 수정
  - app.default-thumbnail-url 속성 추가: `/static/images/default-thumbnail.png`
- **Convention Notes**:
  - 정적 리소스 경로: /static/images/
  - 설정 키: app.default-thumbnail-url (kebab-case)
  - 환경별 설정 가능하도록 application.yml에 정의
- **Verification**:
  - 애플리케이션 시작 후 http://localhost:8080/static/images/default-thumbnail.png 접근 확인
  - @Value("${app.default-thumbnail-url}") 주입 테스트
- **Exit Criteria**: 기본 이미지 접근 가능, 설정 값 정상 주입
- **Status**: pending

### Todo 5: PostListItemResponse DTO 확장
- **Priority**: 2
- **Dependencies**: none (독립적이나 Todo 7 이전에 완료 필요)
- **Goal**: 응답 DTO에 프로필 이미지, 썸네일, 읽음 여부 필드 추가
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/dto/PostListItemResponse.kt` 수정
  - authorProfileImageUrl: String 필드 추가
  - thumbnailUrl: String 필드 추가
  - isRead: Boolean 필드 추가
  - @Schema description 추가 (각 필드의 의미 명시)
  - companion object의 from() 메서드는 Todo 7에서 수정 (현재는 컴파일 에러 방지용 임시값)
- **Convention Notes**:
  - 필드 순서: 기존 필드 유지, 신규 필드는 authorName 다음에 배치
  - Boolean 네이밍: isRead (is 접두사)
  - nullable 대신 기본값: authorProfileImageUrl = "", thumbnailUrl = "", isRead = false
- **Verification**:
  - 컴파일 성공
  - Swagger UI에서 스키마 확인
- **Exit Criteria**: DTO 필드 추가 완료, 컴파일 성공
- **Status**: pending

### Todo 6: PostRepositoryCustomImpl Fetch Join 추가
- **Priority**: 2
- **Dependencies**: Todo 2, Todo 3
- **Goal**: N+1 쿼리 방지를 위해 author, pictures를 Fetch Join으로 조회
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepositoryCustomImpl.kt` 수정
  - findPostsWithConditions 메서드에 Fetch Join 추가:
    - `.leftJoin(post.author).fetchJoin()`
    - `.leftJoin(post.pictures).fetchJoin()`
  - distinct() 추가 (OneToMany Join으로 인한 중복 제거)
  - 기존 where, orderBy 로직 유지
- **Convention Notes**:
  - QueryDSL leftJoin 사용 (null 가능한 관계는 left)
  - fetchJoin()으로 즉시 로딩
  - distinct()로 중복 Post 제거
  - 주석: "author와 pictures를 Fetch Join하여 N+1 쿼리 방지"
- **Verification**:
  - 실행 시 SQL 로그 확인 (p6spy 활용)
  - 단일 쿼리에 LEFT JOIN users, LEFT JOIN post_pictures 포함 확인
  - 결과 중복 없음 확인
- **Exit Criteria**: Fetch Join 정상 동작, N+1 쿼리 없음
- **Status**: pending

### Todo 7: PostListReadService에 읽음 여부 로직 추가
- **Priority**: 3
- **Dependencies**: Todo 5, Todo 6
- **Goal**: 현재 로그인 사용자 기준으로 게시물 읽음 여부 판단 및 DTO 변환
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/application/PostListReadService.kt` 수정
  - SecurityContextHolder에서 현재 인증 정보 추출
  - 인증 정보가 없으면 userId = null (비회원)
  - PostViewLogRepository 주입 추가
  - 조회된 post ID 리스트로 PostViewLog 배치 조회 (WHERE user_id = ? AND post_id IN (?))
  - Map<UUID, Boolean> 형태로 읽음 여부 캐시
  - PostListItemResponse.from() 메서드에 thumbnailUrl, isRead 로직 추가:
    - thumbnailUrl: pictures에서 isThumbnail=true 우선, 없으면 displayOrder 낮은 순, 없으면 defaultThumbnailUrl
    - isRead: readPostIds에 포함 여부로 판단
  - @Value로 defaultThumbnailUrl 주입
- **Convention Notes**:
  - SecurityContextHolder.getContext().authentication 사용
  - authentication?.principal as? User (타입 캐스팅 안전하게)
  - repository.findByUserIdAndPostIdIn() 메서드 활용
  - 주석: "현재 사용자가 조회한 게시물인지 판단하기 위해 PostViewLog 배치 조회"
- **Verification**:
  - 로그인 사용자: isRead가 PostViewLog 기반으로 정확히 설정됨
  - 비회원: isRead 항상 false
  - thumbnailUrl이 우선순위에 따라 정확히 반환됨
  - 기본 이미지 URL이 pictures 없을 때 반환됨
- **Exit Criteria**: 읽음 여부 및 썸네일 로직 정상 동작
- **Status**: pending

### Todo 8: PostViewLogRepository 쿼리 메서드 추가
- **Priority**: 2
- **Dependencies**: none
- **Goal**: 사용자별 게시물 읽음 여부 배치 조회 메서드 구현
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostViewLogRepository.kt` 수정
  - 메서드 추가: `fun existsByUserIdAndPostIdIn(userId: UUID, postIds: List<UUID>): List<PostViewLog>`
  - 또는 `fun findDistinctPostIdsByUserIdAndPostIdIn(userId: UUID, postIds: List<UUID>): List<UUID>`
  - JPA 메서드 네이밍 컨벤션 준수
- **Convention Notes**:
  - Repository는 JpaRepository 상속
  - 메서드명으로 쿼리 자동 생성
  - 반환 타입: List<UUID> (중복 제거된 post_id 목록)
- **Verification**:
  - 메서드 호출 시 SQL 생성 확인
  - WHERE user_id = ? AND post_id IN (?, ?, ...) 쿼리 실행 확인
- **Exit Criteria**: Repository 메서드 정상 동작
- **Status**: pending

### Todo 9: Controller 수정 (Optional - 인증 정보 전달 명시)
- **Priority**: 3
- **Dependencies**: Todo 7
- **Goal**: Controller에서 Service로 인증 정보 전달 (현재는 Service에서 직접 추출하므로 선택적)
- **Work**:
  - `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostController.kt` 확인
  - 현재 getPosts 메서드는 파라미터 변경 불필요 (Service에서 SecurityContext 사용)
  - Swagger @Operation description 업데이트: "로그인 시 읽음 여부 포함, 비회원도 조회 가능"
  - ApiResponse 200 설명에 "작성자 프로필, 썸네일, 읽음 여부 포함" 추가
- **Convention Notes**:
  - Controller는 얇게 유지, 비즈니스 로직은 Service에 위임
  - Swagger description은 한국어로 작성
- **Verification**:
  - Swagger UI에서 API 설명 확인
  - 로그인/비로그인 상태에서 API 호출 테스트
- **Exit Criteria**: API 문서 업데이트 완료, 기능 정상 동작
- **Status**: pending

### Todo 10: Integration Test 작성
- **Priority**: 4
- **Dependencies**: Todo 1~9 전체
- **Goal**: 전체 기능의 정상 동작을 검증하는 통합 테스트 작성
- **Work**:
  - `src/test/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostControllerTest.kt` 수정 또는 신규 테스트 추가
  - BaseIntegrationTest 상속
  - Given-When-Then 패턴으로 테스트 작성
  - 테스트 케이스:
    1. 비회원 조회 시 isRead 항상 false
    2. 로그인 사용자가 읽은 게시물은 isRead true
    3. 썸네일 있는 게시물은 thumbnailUrl 반환
    4. 썸네일 없는 게시물은 기본 이미지 URL 반환
    5. 작성자 프로필 이미지 정상 반환
  - @DisplayName으로 한글 설명 추가
  - AssertJ로 검증
- **Convention Notes**:
  - 테스트 클래스명: PostControllerTest (또는 PostListReadServiceTest)
  - 메서드명: 한글로 명확한 의도 표현
  - @Transactional로 롤백 보장
  - 모든 분기 케이스 테스트
- **Verification**:
  - `./gradlew test` 실행
  - 모든 테스트 통과
  - 커버리지 80% 이상
- **Exit Criteria**: 모든 테스트 통과, 주요 시나리오 검증 완료
- **Status**: pending

## Verification Strategy
전체 구현 완료 후 다음 검증 수행:
1. **Flyway 마이그레이션**: `./gradlew flywayMigrate` 실행, V10 성공 확인
2. **애플리케이션 시작**: 에러 없이 정상 시작
3. **Swagger UI**: http://localhost:8080/swagger-ui.html에서 API 스키마 확인
4. **API 테스트**:
   - 비회원 상태: GET /open-api/posts?size=10 → isRead 모두 false
   - 로그인 상태: 읽은 게시물은 isRead true, 안 읽은 게시물은 false
   - thumbnailUrl 정상 반환 (있는 경우, 없는 경우)
   - authorProfileImageUrl 정상 반환
5. **SQL 로그**: p6spy로 N+1 쿼리 없음 확인
6. **Integration Test**: `./gradlew test` 모두 통과

## Progress Tracking
- Total Todos: 10
- Completed: 9 (Todo 1-9)
- Remaining: 1 (Todo 10 - Integration Test)
- Status: Implementation complete, testing pending

## Change Log
- 2026-02-04: Plan created
- 2026-02-04: Todos 1-9 completed (DB migration, entities, DTO, repository, service, controller)
