# Integration Test Guide - Testcontainers & TestUserFactory

## 개요
**날짜**: 2026-02-06  
**목적**: RestAssured + Testcontainers 통합 테스트 패턴 정립  
**상태**: ✅ COMPLETE

## 핵심 원칙

### 1. @Transactional은 RestAssured 테스트에서 작동하지 않음

**이유:**
- RestAssured는 실제 HTTP 요청을 전송
- HTTP 요청은 **별도의 트랜잭션**에서 처리됨
- 테스트 메서드의 `@Transactional` 롤백이 HTTP 요청 데이터에 영향 없음

**참고 자료:**
- [Quarkus Issue #15436](https://github.com/quarkusio/quarkus/issues/15436)
- [rieckpil.de - Testing Pitfall](https://rieckpil.de/spring-boot-testing-pitfall-transaction-rollback-in-tests/)

### 2. 명시적 Cleanup 필요

RestAssured HTTP 통합 테스트에서는:
- ❌ `@Transactional` 롤백 의존 불가
- ✅ `@BeforeEach`에서 명시적 cleanup 필요
- ✅ `TestUserFactory`를 사용한 중앙화된 데이터 관리

## TestUserFactory 패턴

### 위치
```
main-server/src/test/kotlin/com/techtaurant/mainserver/base/TestUserFactory.kt
```

### 설계 철학

**Singleton 객체 (데이터 생성만 담당, cleanup 없음):**
```kotlin
object TestUserFactory {
    fun createTestUser(userRepository: UserRepository): User
    fun createTestCategory(categoryRepository: CategoryRepository, user: User): Category
    fun createTestPost(postRepository: PostRepository, author: User, category: Category?): Post
}
```

**장점:**
1. ✅ 테스트 데이터 생성 로직 중앙화
2. ✅ 고유한 identifier 자동 생성 (UUID 기반)
3. ✅ 중복 사용자 생성 방지
4. ✅ 일관된 테스트 데이터 보장
5. ✅ cleanup은 각 테스트에서 직접 관리 (관리 비용 최소화)

### 테스트 독립성 전략

**@Transactional 테스트 (서비스, 리포지토리):**
- @Transactional 자동 롤백으로 데이터 격리
- 수동 cleanup 불필요
- UUID identifier로 충돌 방지

**RestAssured HTTP 테스트 (컨트롤러):**
- @BeforeEach에서 deleteAllInBatch() 직접 호출 (외래키 순서 준수)
- UUID identifier로 고유성 보장
- cleanupAllTestData/cleanupDependentData 같은 유틸 메서드 사용 안 함

## 통합 테스트 작성 패턴

### 패턴 1: @BeforeEach 완전 격리 (모든 데이터 매번 재생성)

**특징:**
- 모든 테스트마다 User를 포함한 전체 데이터 재생성
- 완전한 테스트 격리 보장
- User 데이터를 변경하는 테스트에 적합

**구조:**

```kotlin
@DisplayName("Controller 통합 테스트")
class ControllerIntegrationTest : IntegrationTest() {
    
    @Autowired
    private lateinit var commentRepository: CommentRepository
    
    @Autowired
    private lateinit var postRepository: PostRepository
    
    @Autowired
    private lateinit var categoryRepository: CategoryRepository
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider
    
    private lateinit var testUser: User
    private lateinit var testPost: Post
    private lateinit var accessToken: String
    
    @BeforeEach
    fun setUpTestData() {
        // 1. 이전 테스트 데이터 정리
        TestUserFactory.cleanupAllTestData(
            commentRepository = commentRepository,
            postRepository = postRepository,
            categoryRepository = categoryRepository,
            userRepository = userRepository
        )
        
        // 2. 테스트 사용자 생성 (고유 identifier)
        testUser = TestUserFactory.createTestUser(userRepository)
        
        // 3. 테스트 카테고리 생성
        val testCategory = TestUserFactory.createTestCategory(
            categoryRepository, 
            testUser
        )
        
        // 4. 테스트 게시물 생성
        testPost = TestUserFactory.createTestPost(
            postRepository, 
            testUser, 
            testCategory
        )
        
        // 5. JWT 토큰 생성
        accessToken = jwtTokenProvider.createAccessToken(
            testUser.id!!, 
            testUser.role
        )
    }
    
    @Test
    @DisplayName("성공 케이스")
    fun testSuccess() {
        // Given - 테스트 데이터 준비 (@BeforeEach에서 완료)
        val request = CreateRequest(...)
        
        // When - API 호출
        val response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/endpoint")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
        
        // Then - 결과 검증
        assertNotNull(response)
    }
}
```

### IntegrationTest 베이스 클래스

**위치:** `main-server/src/test/kotlin/com/techtaurant/mainserver/base/IntegrationTest.kt`

**설정:**
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // ← application-test.yml 로드
abstract class IntegrationTest {
    
    @LocalServerPort
    protected var port: Int = 0
    
    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.basePath = ""
        RestAssured.baseURI = "http://localhost"
    }
    
    companion object {
        // Testcontainers 싱글톤 - 모든 테스트 간 공유
        private val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("techtaurant_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort())
        
        init {
            postgresContainer.start()
        }
        
        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
        }
    }
}
```

## 주요 주의사항

### 1. @ActiveProfiles("test") 필수

**없으면:**
- `application-test.yml`이 로드되지 않음
- JWT secret 등 테스트 설정이 적용되지 않음
- 인증 실패 가능

**있으면:**
- `application-test.yml` 자동 로드
- 테스트 전용 설정 적용

### 2. Testcontainers는 싱글톤

**설계:**
- `companion object`의 `init` 블록에서 컨테이너 시작
- 모든 테스트 클래스가 동일한 컨테이너 공유
- JVM 종료 시 자동 cleanup

**이점:**
- 테스트 실행 속도 향상
- 리소스 효율적
- 포트 충돌 없음 (동적 할당)

### 3. 외래키 제약조건 순서

**삭제 순서:**
```
댓글 → 게시물 → 카테고리 → 사용자
(참조하는 쪽) → (참조되는 쪽)
```

**생성 순서:**
```
사용자 → 카테고리 → 게시물 → 댓글
(참조되는 쪽) → (참조하는 쪽)
```

## 테스트 실행

### 단일 테스트
```bash
./gradlew test --tests "*ControllerIntegrationTest"
```

### 전체 테스트
```bash
./gradlew test
```

### 디버그 로그
```bash
./gradlew test --info
```

## 트러블슈팅

### 1. Duplicate Key Constraint 에러

**증상:**
```
ERROR: duplicate key value violates unique constraint "users_identifier_provider_key"
```

**원인:**
- 이전 테스트 데이터가 남아있음
- `cleanupAllTestData()` 누락

**해결:**
```kotlin
@BeforeEach
fun setUp() {
    TestUserFactory.cleanupAllTestData(...)  // ← 추가
}
```

### 2. 401 Unauthorized 에러

**증상:**
- JWT 토큰을 전달했는데 401 발생

**원인:**
- `@ActiveProfiles("test")` 누락
- `application-test.yml`의 JWT secret 미로드

**해결:**
```kotlin
@SpringBootTest(...)
@ActiveProfiles("test")  // ← 추가
abstract class IntegrationTest
```

### 3. 외래키 제약 위반

**증상:**
```
ERROR: update or delete on table violates foreign key constraint
```

**원인:**
- cleanup 순서가 잘못됨

**해결:**
```kotlin
// 올바른 순서: 참조하는 쪽 먼저 삭제
commentRepository.deleteAllInBatch()  // 1
postRepository.deleteAllInBatch()      // 2
categoryRepository.deleteAllInBatch()  // 3
userRepository.deleteAllInBatch()      // 4
```

## 파일 구조

```
main-server/src/test/kotlin/com/techtaurant/mainserver/
├── base/
│   ├── IntegrationTest.kt           # 베이스 클래스 (Testcontainers)
│   ├── TestUserFactory.kt           # 테스트 데이터 생성 Singleton
│   └── TestHelper.kt                # RestAssured 헬퍼
│
├── comment/infrastructure/in/
│   └── CommentControllerIntegrationTest.kt
│
├── post/infrastructure/in/
│   └── PostControllerTest.kt
│
└── resources/
    └── application-test.yml         # 테스트 전용 설정
```

### 패턴 2: @BeforeAll 공유 User (User 재사용, 나머지 매번 재생성) - 권장

**특징:**
- User를 @BeforeAll로 한 번만 생성하여 모든 테스트에서 재사용
- 댓글/게시물/카테고리는 매 테스트마다 재생성
- **User 생성 오버헤드 제거**로 테스트 성능 향상
- User 데이터를 **읽기 전용**으로 사용하는 테스트에 적합

**구조:**
```kotlin
@DisplayName("Controller 통합 테스트")
class ControllerIntegrationTest : IntegrationTest() {
    
    @Autowired
    private lateinit var commentRepository: CommentRepository
    
    @Autowired
    private lateinit var postRepository: PostRepository
    
    @Autowired
    private lateinit var categoryRepository: CategoryRepository
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider
    
    private lateinit var testPost: Post
    private lateinit var accessToken: String
    
    companion object {
        lateinit var sharedTestUser: User
        
        @BeforeAll
        @JvmStatic
        fun setUpSharedUser(@Autowired userRepository: UserRepository) {
            // User를 한 번만 생성 (모든 테스트에서 공유)
            sharedTestUser = TestUserFactory.createTestUser(userRepository)
        }
        
        @AfterAll
        @JvmStatic
        fun tearDownSharedUser(
            @Autowired commentRepository: CommentRepository,
            @Autowired postRepository: PostRepository,
            @Autowired categoryRepository: CategoryRepository,
            @Autowired userRepository: UserRepository
        ) {
            // 모든 테스트 종료 후 cleanup
            TestUserFactory.cleanupAllTestData(
                commentRepository, postRepository, categoryRepository, userRepository
            )
        }
    }
    
    @BeforeEach
    fun setUpTestData() {
        // User는 유지, 나머지만 cleanup
        TestUserFactory.cleanupDependentData(
            commentRepository = commentRepository,
            postRepository = postRepository,
            categoryRepository = categoryRepository
        )
        
        // 테스트 데이터 생성 (공유 User 사용)
        val testCategory = TestUserFactory.createTestCategory(
            categoryRepository, 
            sharedTestUser
        )
        testPost = TestUserFactory.createTestPost(
            postRepository, 
            sharedTestUser, 
            testCategory
        )
        
        // JWT 토큰 생성
        accessToken = jwtTokenProvider.createAccessToken(
            sharedTestUser.id!!, 
            sharedTestUser.role
        )
    }
    
    @Test
    @DisplayName("성공 케이스")
    fun testSuccess() {
        // Given - 테스트 데이터 준비 (@BeforeEach에서 완료)
        val request = CreateRequest(...)
        
        // When - API 호출
        val response = given()
            .contentType(ContentType.JSON)
            .body(request)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/endpoint")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
        
        // Then - 결과 검증
        assertNotNull(response)
    }
}
```

**장점:**
- ✅ User 생성 오버헤드 제거 (N번 → 1번)
- ✅ cleanup 범위 축소 (4개 테이블 → 3개 테이블)
- ✅ 테스트 실행 속도 향상

**주의사항:**
- ⚠️ User 데이터를 변경하는 테스트는 패턴 1 사용 필요
- ⚠️ `@BeforeAll`은 `companion object`에 `@JvmStatic`으로 정의

**패턴 선택 가이드:**
| 테스트 유형 | 권장 패턴 |
|-----------|---------|
| 댓글/게시물 조회/생성/삭제 | 패턴 2 (공유 User) |
| User 프로필 수정/삭제 | 패턴 1 (완전 격리) |
| 권한 변경 테스트 | 패턴 1 (완전 격리) |

## 예제: CommentControllerIntegrationTest

```kotlin
@DisplayName("CommentController 통합 테스트")
class CommentControllerIntegrationTest : IntegrationTest() {
    
    @Autowired
    private lateinit var commentRepository: CommentRepository
    
    @Autowired
    private lateinit var postRepository: PostRepository
    
    @Autowired
    private lateinit var categoryRepository: CategoryRepository
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider
    
    private lateinit var testUser: User
    private lateinit var testPost: Post
    private lateinit var accessToken: String
    
    @BeforeEach
    fun setUpTestData() {
        // TestUserFactory 사용
        TestUserFactory.cleanupAllTestData(
            commentRepository = commentRepository,
            postRepository = postRepository,
            categoryRepository = categoryRepository,
            userRepository = userRepository
        )
        
        testUser = TestUserFactory.createTestUser(userRepository)
        val testCategory = TestUserFactory.createTestCategory(categoryRepository, testUser)
        testPost = TestUserFactory.createTestPost(postRepository, testUser, testCategory)
        accessToken = jwtTokenProvider.createAccessToken(testUser.id!!, testUser.role)
    }
    
    @Test
    @DisplayName("댓글 작성 성공")
    fun createComment_withValidRequest_shouldReturnCreated() {
        val request = CreateCommentRequest(
            content = "좋은 글이네요!",
            postId = testPost.id!!,
            parentId = null
        )
        
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/comments")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("data.content", equalTo("좋은 글이네요!"))
    }
}
```

## 베스트 프랙티스

### DO ✅

1. **TestUserFactory 사용**
   - 중앙화된 데이터 생성
   - 고유 identifier 자동 생성
   - 외래키 순서 자동 관리

2. **적절한 패턴 선택**
   - User 읽기 전용 → 패턴 2 (@BeforeAll 공유 User) - 성능 우수
   - User 변경 필요 → 패턴 1 (@BeforeEach 완전 격리) - 격리 보장

3. **명시적 cleanup**
   - 매 테스트 전 데이터 정리
   - 테스트 격리 보장
   - `cleanupDependentData()` 또는 `cleanupAllTestData()` 사용

4. **@ActiveProfiles("test")**
   - 테스트 전용 설정 로드
   - JWT secret 등 설정 적용

5. **IntegrationTest 상속**
   - Testcontainers 자동 설정
   - RestAssured 자동 설정

### DON'T ❌

1. **@Transactional 의존**
   - RestAssured HTTP 테스트에서 작동 안 함
   - 명시적 cleanup 필요

2. **하드코딩된 identifier**
   ```kotlin
   // ❌ 중복 가능
   identifier = "google-test-id"
   
   // ✅ 고유 보장
   identifier = "test-id-${UUID.randomUUID()}"
   ```

3. **cleanup 순서 무시**
   - 외래키 제약 위반 발생
   - TestUserFactory.cleanupAllTestData() 사용

4. **@ActiveProfiles 누락**
   - application-test.yml 미로드
   - 인증/설정 문제 발생

## 참고 자료

- [Spring Boot Testing Pitfall](https://rieckpil.de/spring-boot-testing-pitfall-transaction-rollback-in-tests/)
- [Quarkus RestAssured Rollback Issue](https://github.com/quarkusio/quarkus/issues/15436)
- [Baeldung - Spring Test Transactions](https://www.baeldung.com/spring-test-programmatic-transactions)

## 결론

**핵심:**
1. ✅ RestAssured HTTP 테스트에서는 `@Transactional` 롤백 불가
2. ✅ `TestUserFactory`로 중앙화된 데이터 관리
3. ✅ 두 가지 패턴 제공:
   - **패턴 1**: @BeforeEach 완전 격리 (User 변경 테스트용)
   - **패턴 2**: @BeforeAll 공유 User (성능 최적화, 읽기 전용 권장)
4. ✅ `@ActiveProfiles("test")`로 테스트 설정 로드
5. ✅ Testcontainers 싱글톤으로 효율적 리소스 사용

**결과:**
- 🎯 테스트 격리 보장
- 🎯 중복 데이터 방지
- 🎯 일관된 테스트 환경
- 🎯 빠른 테스트 실행
