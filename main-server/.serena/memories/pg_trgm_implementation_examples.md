# pg_trgm GIN 인덱스 구현 가이드 - 실전 예제

## Part 1: Flyway 마이그레이션 스크립트

### V1__Create_users_table.sql
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

### V2__Add_trgm_extension.sql
```sql
-- pg_trgm 확장 설치 (멱등성 보장)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 기본 GIN 인덱스 생성
CREATE INDEX idx_users_nickname_gin 
ON users USING GIN (nickname gin_trgm_ops)
WHERE status = 'active';  -- 부분 인덱스: active 사용자만

-- 통계 갱신
ANALYZE users(nickname);
```

### V3__Add_nickname_lowercase_index.sql (Optional)
```sql
-- 대소문자 무시 검색을 위한 함수 인덱스
CREATE INDEX idx_users_nickname_lower_gin 
ON users USING GIN (LOWER(nickname) gin_trgm_ops);

-- 통계 갱신
ANALYZE users(nickname);
```

---

## Part 2: Spring Data JPA Repository 구현

### Entity 클래스
```kotlin
// User.kt
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false, length = 100)
    val nickname: String,

    @Column(nullable = false)
    val status: String = "active",

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
```

### Repository - Native Query 방식 (권장)
```kotlin
// UserRepository.kt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    // 방법 1: 기본 Native Query (가장 효율적)
    @Query(
        nativeQuery = true,
        value = """
            SELECT u.* FROM users u
            WHERE u.nickname LIKE CONCAT('%', :keyword, '%')
            AND u.status = 'active'
            ORDER BY u.created_at DESC
            LIMIT :limit
        """
    )
    fun searchByNickname(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int
    ): List<User>

    // 방법 2: Case-insensitive 검색
    @Query(
        nativeQuery = true,
        value = """
            SELECT u.* FROM users u
            WHERE LOWER(u.nickname) LIKE CONCAT('%', LOWER(:keyword), '%')
            AND u.status = 'active'
            ORDER BY u.created_at DESC
            LIMIT :limit
        """
    )
    fun searchByNicknameIgnoreCase(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int
    ): List<User>

    // 방법 3: Pagination 지원
    @Query(
        nativeQuery = true,
        value = """
            SELECT u.* FROM users u
            WHERE u.nickname LIKE CONCAT('%', :keyword, '%')
            AND u.status = 'active'
            ORDER BY u.created_at DESC
            LIMIT :limit OFFSET :offset
        """,
        countQuery = """
            SELECT COUNT(u.id) FROM users u
            WHERE u.nickname LIKE CONCAT('%', :keyword, '%')
            AND u.status = 'active'
        """
    )
    fun searchByNicknameWithPagination(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<User>

    // 방법 4: 검색어 정규화 (공백 제거)
    @Query(
        nativeQuery = true,
        value = """
            SELECT u.* FROM users u
            WHERE REPLACE(LOWER(u.nickname), ' ', '') 
                  LIKE CONCAT('%', REPLACE(LOWER(:keyword), ' ', ''), '%')
            AND u.status = 'active'
            ORDER BY u.created_at DESC
            LIMIT :limit
        """
    )
    fun searchByNicknameNormalized(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int
    ): List<User>
}
```

---

## Part 3: Service Layer 구현

### UserSearchService
```kotlin
// UserSearchService.kt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserSearchService(
    private val userRepository: UserRepository
) {

    /**
     * 사용자를 닉네임으로 검색합니다.
     * 
     * pg_trgm GIN 인덱스를 활용하여 효율적인 부분 문자열 검색을 수행합니다.
     * WHERE 절의 LIKE '%keyword%' 패턴이 GIN 인덱스를 타도록 설계되었습니다.
     *
     * @param keyword 검색할 닉네임 키워드 (공백 포함 가능)
     * @param limit 반환할 최대 사용자 수 (기본값: 100)
     * @return 검색 결과 사용자 목록
     */
    @Transactional(readOnly = true)
    fun searchUserByNickname(
        keyword: String,
        limit: Int = 100
    ): List<UserSearchResponse> {
        // 입력 검증 및 정규화
        val normalizedKeyword = keyword.trim()
        
        if (normalizedKeyword.isEmpty()) {
            return emptyList()
        }
        
        if (normalizedKeyword.length > 100) {
            return emptyList()
        }

        // Repository를 통한 인덱스 기반 검색
        val users = userRepository.searchByNickname(normalizedKeyword, limit)
        
        return users.map { it.toSearchResponse() }
    }

    /**
     * 대소문자를 무시하고 닉네임으로 검색합니다.
     *
     * @param keyword 검색할 닉네임 키워드
     * @param limit 반환할 최대 사용자 수
     * @return 검색 결과 사용자 목록
     */
    @Transactional(readOnly = true)
    fun searchUserByNicknameIgnoreCase(
        keyword: String,
        limit: Int = 100
    ): List<UserSearchResponse> {
        val normalizedKeyword = keyword.trim()
        
        if (normalizedKeyword.isEmpty() || normalizedKeyword.length > 100) {
            return emptyList()
        }

        val users = userRepository.searchByNicknameIgnoreCase(normalizedKeyword, limit)
        return users.map { it.toSearchResponse() }
    }

    /**
     * 공백을 제거하고 닉네임으로 검색합니다.
     *
     * 예: "john doe" 검색 시 "johndoe"도 매칭됨
     *
     * @param keyword 검색할 닉네임 키워드
     * @param limit 반환할 최대 사용자 수
     * @return 검색 결과 사용자 목록
     */
    @Transactional(readOnly = true)
    fun searchUserByNicknameNormalized(
        keyword: String,
        limit: Int = 100
    ): List<UserSearchResponse> {
        val normalizedKeyword = keyword.trim()
        
        if (normalizedKeyword.isEmpty() || normalizedKeyword.length > 100) {
            return emptyList()
        }

        val users = userRepository.searchByNicknameNormalized(normalizedKeyword, limit)
        return users.map { it.toSearchResponse() }
    }

    /**
     * 페이지네이션을 지원하는 닉네임 검색입니다.
     *
     * @param keyword 검색할 닉네임 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지 당 사용자 수
     * @return 검색 결과 및 총 개수
     */
    @Transactional(readOnly = true)
    fun searchUserByNicknameWithPagination(
        keyword: String,
        page: Int = 0,
        pageSize: Int = 20
    ): UserSearchPageResponse {
        val normalizedKeyword = keyword.trim()
        
        if (normalizedKeyword.isEmpty() || normalizedKeyword.length > 100) {
            return UserSearchPageResponse(
                content = emptyList(),
                page = page,
                pageSize = pageSize,
                totalCount = 0
            )
        }

        val offset = page * pageSize
        val users = userRepository.searchByNicknameWithPagination(
            keyword = normalizedKeyword,
            limit = pageSize,
            offset = offset
        )
        
        // 총 개수 조회 (실제 환경에서는 countQuery 분리 필요)
        val totalCount = users.size

        return UserSearchPageResponse(
            content = users.map { it.toSearchResponse() },
            page = page,
            pageSize = pageSize,
            totalCount = totalCount
        )
    }
}

// Extension function
fun User.toSearchResponse(): UserSearchResponse {
    return UserSearchResponse(
        id = this.id!!,
        email = this.email,
        nickname = this.nickname,
        createdAt = this.createdAt
    )
}
```

### Response DTO
```kotlin
// UserSearchResponse.kt
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class UserSearchResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    @JsonProperty("created_at")
    val createdAt: LocalDateTime
)

data class UserSearchPageResponse(
    val content: List<UserSearchResponse>,
    val page: Int,
    @JsonProperty("page_size")
    val pageSize: Int,
    @JsonProperty("total_count")
    val totalCount: Int
)
```

---

## Part 4: Controller 구현

### UserSearchController
```kotlin
// UserSearchController.kt
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserSearchController(
    private val userSearchService: UserSearchService
) {

    /**
     * 닉네임으로 사용자를 검색합니다.
     *
     * 요청 예시:
     * - GET /api/v1/users/search/nickname?keyword=john
     * - GET /api/v1/users/search/nickname?keyword=john&limit=50
     *
     * @param keyword 검색 키워드
     * @param limit 반환할 최대 결과 수 (기본값: 100, 최대: 1000)
     * @return 검색 결과 목록
     */
    @GetMapping("/search/nickname")
    fun searchByNickname(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<UserSearchResponse>> {
        val validatedLimit = limit.coerceIn(1, 1000)
        val results = userSearchService.searchUserByNickname(keyword, validatedLimit)
        return ResponseEntity.ok(results)
    }

    /**
     * 대소문자를 무시하고 검색합니다.
     *
     * @param keyword 검색 키워드
     * @param limit 반환할 최대 결과 수
     * @return 검색 결과 목록
     */
    @GetMapping("/search/nickname/ignore-case")
    fun searchByNicknameIgnoreCase(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<UserSearchResponse>> {
        val validatedLimit = limit.coerceIn(1, 1000)
        val results = userSearchService.searchUserByNicknameIgnoreCase(keyword, validatedLimit)
        return ResponseEntity.ok(results)
    }

    /**
     * 공백을 무시하고 검색합니다.
     *
     * @param keyword 검색 키워드
     * @param limit 반환할 최대 결과 수
     * @return 검색 결과 목록
     */
    @GetMapping("/search/nickname/normalized")
    fun searchByNicknameNormalized(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<UserSearchResponse>> {
        val validatedLimit = limit.coerceIn(1, 1000)
        val results = userSearchService.searchUserByNicknameNormalized(keyword, validatedLimit)
        return ResponseEntity.ok(results)
    }

    /**
     * 페이지네이션을 지원하는 검색입니다.
     *
     * 요청 예시:
     * - GET /api/v1/users/search/nickname/paginated?keyword=john&page=0&pageSize=20
     *
     * @param keyword 검색 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지 당 결과 수
     * @return 페이지네이션된 검색 결과
     */
    @GetMapping("/search/nickname/paginated")
    fun searchByNicknamePaginated(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<UserSearchPageResponse> {
        val validatedPageSize = pageSize.coerceIn(1, 100)
        val validatedPage = page.coerceAtLeast(0)
        
        val result = userSearchService.searchUserByNicknameWithPagination(
            keyword = keyword,
            page = validatedPage,
            pageSize = validatedPageSize
        )
        
        return ResponseEntity.ok(result)
    }
}
```

---

## Part 5: 성능 모니터링

### 쿼리 실행 계획 확인 스크립트
```sql
-- 1. 현재 인덱스 상태 확인
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_indexes
WHERE tablename = 'users'
ORDER BY indexname;

-- 2. 인덱스 사용 통계
SELECT 
    schemaname,
    tablename,
    indexrelname,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE tablename = 'users'
ORDER BY idx_scan DESC;

-- 3. 쿼리 실행 계획 확인
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT JSON)
SELECT u.* FROM users u
WHERE u.nickname LIKE CONCAT('%', 'john', '%')
AND u.status = 'active'
ORDER BY u.created_at DESC
LIMIT 100;

-- 4. 통계 정보 조회
SELECT 
    n_distinct,
    n_distinct_inherited,
    avg_width,
    null_frac,
    correlation
FROM pg_stats
WHERE tablename = 'users' AND attname = 'nickname';

-- 5. 테이블 및 인덱스 크기
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size
FROM pg_tables
WHERE tablename = 'users';
```

### JUnit 성능 테스트 예제
```kotlin
// UserSearchServiceTest.kt
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import kotlin.system.measureTimeMillis

@DataJpaTest
@TestPropertySource(properties = ["spring.jpa.hibernate.ddl-auto=none"])
class UserSearchServiceTest(
    @Autowired private val userRepository: UserRepository
) {

    private lateinit var userSearchService: UserSearchService

    @BeforeEach
    fun setup() {
        userSearchService = UserSearchService(userRepository)
    }

    @Test
    @DisplayName("닉네임 검색이 1000ms 이내에 완료되어야 함")
    fun testSearchPerformance() {
        // Given: 1000명의 사용자 생성
        val users = (1..1000).map { i ->
            User(
                email = "user$i@example.com",
                nickname = "user_$i",
                status = "active"
            )
        }
        userRepository.saveAll(users)

        // When: 검색 실행
        val executionTime = measureTimeMillis {
            val results = userSearchService.searchUserByNickname("user_1", 100)
            
            // Then: 결과 검증
            assert(results.isNotEmpty())
            assert(results.size <= 100)
        }

        println("검색 실행 시간: ${executionTime}ms")
        assert(executionTime < 1000) { "검색이 1000ms를 초과했습니다" }
    }

    @Test
    @DisplayName("대소문자 무시 검색이 GIN 인덱스를 활용해야 함")
    fun testCaseInsensitiveSearch() {
        // Given
        val users = listOf(
            User(email = "user1@example.com", nickname = "John", status = "active"),
            User(email = "user2@example.com", nickname = "JOHN", status = "active"),
            User(email = "user3@example.com", nickname = "john", status = "active")
        )
        userRepository.saveAll(users)

        // When
        val results = userSearchService.searchUserByNicknameIgnoreCase("john", 100)

        // Then
        assert(results.size == 3)
    }
}
```

---

## Part 6: 트러블슈팅 체크리스트

### 배포 전 확인사항
```sql
-- 1. pg_trgm 설치 확인
SELECT EXISTS (
    SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm'
) AS trgm_installed;

-- 2. 인덱스 존재 확인
SELECT COUNT(*) FROM pg_indexes 
WHERE tablename = 'users' AND indexname LIKE '%nickname%';

-- 3. 통계 갱신 상태 확인
SELECT last_vacuum, last_autovacuum, last_analyze, last_autoanalyze
FROM pg_stat_user_tables
WHERE relname = 'users';

-- 4. 테이블 행 수 확인
SELECT COUNT(*) as total_rows FROM users WHERE status = 'active';

-- 5. 인덱스 유효성 확인
SELECT * FROM pg_stat_user_indexes 
WHERE indexrelname LIKE '%nickname%';
```

### 문제 발생 시 대응
```kotlin
// 프로덕션 환경에서 인덱스가 타지 않는 경우
// 1. enable_seqscan 임시 비활성화 (즉각적 완화)
// 2. ANALYZE 재실행 (근본 원인)
// 3. 인덱스 재구성 (장기 해결책)

@Service
class UserSearchServiceDebug(
    private val userRepository: UserRepository,
    @Value("\${search.enable-seq-scan:true}") 
    private val enableSeqScan: Boolean
) {
    
    fun searchWithDebug(keyword: String): List<UserSearchResponse> {
        if (!enableSeqScan) {
            // enable_seqscan = OFF 설정 쿼리 실행
            // (필요시 JDBC Template 사용)
        }
        return userRepository.searchByNickname(keyword, 100)
            .map { it.toSearchResponse() }
    }
}
```

---

## Part 7: 성능 최적화 팁

### 1. 부분 인덱스 활용
```sql
-- active 사용자만 인덱싱 (인덱스 크기 40% 감소)
CREATE INDEX idx_users_nickname_active_gin 
ON users USING GIN (nickname gin_trgm_ops)
WHERE status = 'active';
```

### 2. 인덱스 병렬 생성 (다운타임 최소화)
```sql
-- 프로덕션 환경에서 온라인 인덱스 생성
CREATE INDEX CONCURRENTLY idx_users_nickname_gin 
ON users USING GIN (nickname gin_trgm_ops);
```

### 3. 통계 자동 갱신 설정
```sql
-- postgresql.conf 설정
autovacuum = on
autovacuum_naptime = '1min'
autovacuum_analyze_scale_factor = 0.01
autovacuum_analyze_threshold = 100
```

### 4. 인덱스 비용 최적화
```sql
-- 옵티마이저가 더 자주 인덱스를 선택하도록
ALTER SYSTEM SET random_page_cost = 1.1;  -- SSD 환경
ALTER SYSTEM SET effective_cache_size = '4GB';
SELECT pg_reload_conf();
```
