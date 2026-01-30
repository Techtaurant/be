# pg_trgm GIN 인덱스 - 완벽 가이드 요약

## 📋 5단계 빠른 시작

### 1단계: 확장 설치 (Flyway Migration)
```sql
-- V1__create_pg_trgm_extension.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

### 2단계: GIN 인덱스 생성
```sql
-- V2__create_nickname_gin_index.sql
CREATE INDEX CONCURRENTLY idx_users_nickname_gin 
ON users USING GIN (nickname gin_trgm_ops)
WHERE status = 'active';
```

### 3단계: 통계 갱신
```sql
ANALYZE users(nickname);
```

### 4단계: 쿼리 작성 (Repository)
```kotlin
@Query(nativeQuery = true, value = """
    SELECT u.* FROM users u
    WHERE u.nickname LIKE CONCAT('%', :keyword, '%')
    ORDER BY u.created_at DESC
    LIMIT :limit
""")
fun searchByNickname(
    @Param("keyword") keyword: String,
    @Param("limit") limit: Int
): List<User>
```

### 5단계: 실행 계획 확인
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM users WHERE nickname LIKE '%john%';
-- 결과: "Bitmap Index Scan" → 인덱스 정상 동작
```

---

## 🔍 Seq Scan vs Index Scan 비교

| 항목 | Seq Scan | Index Scan (우수) |
|------|----------|------------------|
| 행 수 1,000 | 0.5ms | 2ms (느림) |
| 행 수 10,000 | 5ms | 1ms (빠름) |
| 행 수 100,000 | 50ms | 2ms (매우 빠름) |
| 인덱스 크기 | 0 | 큼 |
| 쓰기 성능 | 빠름 | 느림 |

**결론**: 데이터 10,000행 이상에서 Index Scan 필수

---

## ⚠️ Seq Scan이 발생하는 5가지 원인

### 원인 1️⃣: pg_trgm 확장 미설치
```sql
-- 확인
SELECT * FROM pg_extension WHERE extname = 'pg_trgm';

-- 해결
CREATE EXTENSION pg_trgm;
```

### 원인 2️⃣: GIN 인덱스 부재
```sql
-- 확인
SELECT * FROM pg_indexes WHERE tablename = 'users';

-- 해결
CREATE INDEX idx_users_nickname_gin 
ON users USING GIN (nickname gin_trgm_ops);
```

### 원인 3️⃣: 통계 정보 부실
```sql
-- 확인
SELECT last_analyze FROM pg_stat_user_tables 
WHERE relname = 'users';

-- 해결
ANALYZE users;
```

### 원인 4️⃣: 데이터 크기 너무 작음
- 행 수 < 1000: 옵티마이저가 Seq Scan을 선택할 수 있음
- 행 수 < 100: 거의 항상 Seq Scan 선택 (정상)

### 원인 5️⃣: 옵티마이저 비용 설정 부정확
```sql
-- 일시 해결 (임시방편)
SET enable_seqscan = OFF;

-- 근본 해결 (권장)
ALTER SYSTEM SET random_page_cost = 1.1;
SELECT pg_reload_conf();
```

---

## 🎯 올바른 쿼리 작성 패턴

### ✅ 권장하는 방식

#### 패턴 A: 기본 LIKE 검색 (가장 효율적)
```sql
SELECT * FROM users 
WHERE nickname LIKE '%' || ? || '%'
AND status = 'active'
ORDER BY created_at DESC;
-- GIN 인덱스 사용: ✅
-- 결과 수: 높은 선택도
```

#### 패턴 B: 대소문자 무시 (함수 인덱스 필요)
```sql
-- 먼저 함수 인덱스 생성
CREATE INDEX idx_users_nickname_lower_gin 
ON users USING GIN (LOWER(nickname) gin_trgm_ops);

-- 쿼리
SELECT * FROM users 
WHERE LOWER(nickname) LIKE '%' || LOWER(?) || '%';
-- GIN 인덱스 사용: ✅
```

#### 패턴 C: 공백 정규화
```sql
SELECT * FROM users 
WHERE REPLACE(LOWER(nickname), ' ', '') 
      LIKE '%' || REPLACE(LOWER(?), ' ', '') || '%';
-- 직접 GIN 인덱스 미사용
-- 대신 인덱스로 후보 필터링 후 재검증
```

### ❌ 피해야 할 방식

```sql
-- ❌ 함수로 감싸기
WHERE CONCAT('%', nickname, '%') LIKE ?
-- 문제: 함수로 감싸면 인덱스 미사용

-- ❌ 형변환
WHERE CAST(nickname AS VARCHAR) LIKE '%word%'
-- 문제: 형변환으로 인덱스 미사용

-- ❌ OR 조건 (별도 인덱스 스캔 반복)
WHERE nickname LIKE '%john%' OR nickname LIKE '%jane%'
-- 해결: 정규 표현식 사용 또는 UNION
SELECT * FROM users WHERE nickname LIKE '%john%'
UNION
SELECT * FROM users WHERE nickname LIKE '%jane%'
```

---

## 📊 EXPLAIN 분석 가이드

### 정상 쿼리 플랜 (Index Scan)
```
Bitmap Index Scan on idx_users_nickname_gin
  Index Cond: (nickname ~~* '%john%')
→ Bitmap Heap Scan on users
    Recheck Cond: (nickname ~~* '%john%')
    Rows: 10  Width: 32
```

**의미:**
- "Bitmap Index Scan" = GIN 인덱스 사용 ✅
- "Index Cond" = 인덱스 조건으로 후보 행 추출
- "Recheck Cond" = 실제 LIKE로 재검증
- 총 비용: 4.23..12.50

### 문제 있는 쿼리 플랜 (Seq Scan)
```
Seq Scan on users
  Filter: (nickname LIKE '%john%')
  Rows: 10  Width: 32
```

**의미:**
- "Seq Scan" = 전체 테이블 스캔 (인덱스 미사용) ⚠️
- "Filter" = 메모리에서 필터링
- 총 비용: 0.00..35.50 (낮아 보이지만 느림)

### EXPLAIN 명령어 비교
```sql
-- 기본 플랜만 보기
EXPLAIN SELECT * FROM users WHERE nickname LIKE '%john%';

-- 실제 실행 시간 포함 (가장 유용)
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM users WHERE nickname LIKE '%john%';

-- JSON 형식 (프로그래밍)
EXPLAIN (FORMAT JSON, ANALYZE) SELECT * FROM users WHERE nickname LIKE '%john%';
```

---

## 🚀 Spring Boot 구현 완벽 예제

### Repository
```kotlin
@Repository
interface UserRepository : JpaRepository<User, Long> {
    @Query(nativeQuery = true, value = """
        SELECT u.* FROM users u
        WHERE u.nickname LIKE CONCAT('%', :keyword, '%')
        AND u.status = 'active'
        ORDER BY u.created_at DESC
        LIMIT :limit
    """)
    fun searchByNickname(
        @Param("keyword") keyword: String,
        @Param("limit") limit: Int
    ): List<User>
}
```

### Service
```kotlin
@Service
class UserSearchService(
    private val userRepository: UserRepository
) {
    fun searchUserByNickname(keyword: String, limit: Int = 100): List<UserSearchResponse> {
        val normalized = keyword.trim()
        if (normalized.isEmpty() || normalized.length > 100) return emptyList()
        
        return userRepository.searchByNickname(normalized, limit)
            .map { UserSearchResponse(it.id!!, it.email, it.nickname, it.createdAt) }
    }
}
```

### Controller
```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserSearchController(
    private val userSearchService: UserSearchService
) {
    @GetMapping("/search/nickname")
    fun searchByNickname(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<UserSearchResponse>> {
        val validatedLimit = limit.coerceIn(1, 1000)
        val results = userSearchService.searchUserByNickname(keyword, validatedLimit)
        return ResponseEntity.ok(results)
    }
}
```

---

## 📈 성능 모니터링 SQL

### 인덱스 상태 점검
```sql
-- 1. 인덱스 존재 확인
SELECT * FROM pg_indexes 
WHERE tablename = 'users' AND indexname LIKE '%nickname%';

-- 2. 인덱스 사용 통계
SELECT 
    indexrelname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
WHERE indexrelname LIKE '%nickname%';

-- 3. 테이블 행 수
SELECT COUNT(*) FROM users WHERE status = 'active';

-- 4. 통계 갱신 시점
SELECT last_analyze, last_autoanalyze 
FROM pg_stat_user_tables 
WHERE relname = 'users';

-- 5. 최근 쿼리 비용 확인
EXPLAIN ANALYZE SELECT * FROM users WHERE nickname LIKE '%john%';
```

---

## 🔧 배포 전 체크리스트

- [ ] pg_trgm 확장 설치 (`CREATE EXTENSION pg_trgm;`)
- [ ] GIN 인덱스 생성 (`CREATE INDEX ... USING GIN ...;`)
- [ ] 통계 갱신 (`ANALYZE users;`)
- [ ] 쿼리 플랜 확인 (`EXPLAIN ANALYZE ...`)
- [ ] 인덱스 사용 통계 확인 (`pg_stat_user_indexes`)
- [ ] Spring Boot Repository 작성 (Native Query)
- [ ] Controller 검증 로직 추가
- [ ] 성능 테스트 실행

---

## 💡 핵심 요점 정리

### GIN 인덱스 동작 원리
```
검색어 "john" → 트리그램 분해 → " j", "jo", "ohn", "hn " 
→ GIN 인덱스에서 이 트리그램 포함 행 찾기 
→ 교집합 계산 
→ LIKE '%john%'로 재검증 
→ 최종 결과
```

### LIKE '%word%' 인덱스 타는 조건
1. pg_trgm 확장 설치 ✅
2. GIN 인덱스 존재 ✅
3. LIKE 패턴 사용 ✅
4. 통계 최신 ✅
5. 데이터 충분 (1000행 이상) ✅

### enable_seqscan = OFF의 진실
- ❌ 임시 완화책일 뿐
- ✅ 진정한 해결책: ANALYZE + 인덱스 생성
- ⚠️ 프로덕션에서는 enable_seqscan 건드리지 말 것

### 데이터 크기별 전략
| 행 수 | 권장 방식 |
|------|---------|
| < 1000 | Seq Scan도 충분, 인덱스 선택사항 |
| 1000 - 10000 | GIN 인덱스 권장, ANALYZE 필수 |
| 10000+ | GIN 인덱스 필수, 부분 인덱스 고려 |

---

## 🎓 학습 로드맵

1. **기초**: PostgreSQL 인덱스 구조 이해
2. **심화**: 트리그램(Trigram) 동작 원리
3. **실전**: GIN 인덱스 생성 및 테스트
4. **최적화**: EXPLAIN 분석 및 쿼리 튜닝
5. **모니터링**: pg_stat_* 뷰로 성능 추적

---

## 📚 참고 자료

### PostgreSQL 공식 문서
- https://www.postgresql.org/docs/current/pgtrgm.html
- https://www.postgresql.org/docs/current/gist-intro.html

### 핵심 개념
- **Trigram**: 연속된 3글자 조합
- **GIN**: Generalized Inverted Index (역인덱스)
- **GIST**: Generalized Search Tree (균형 트리)
- **Selectivity**: 쿼리 결과가 전체의 몇 %인지

### 관련 연산자
- `LIKE '%word%'`: 트리그램 사용
- `<->`: 트리그램 거리 (Levenshtein)
- `%`: 트리그램 유사도

---

## ❓ FAQ

### Q: pg_trgm 설치하면 성능이 저하되나?
**A**: 아니오. 오직 GIN 인덱스 생성/유지 시에만 약간의 오버헤드 있음. SELECT는 더 빨라짐.

### Q: GIST vs GIN?
**A**: GIN이 검색 속도는 빠르지만, GIST가 업데이트에는 더 유리. 보통 GIN 추천.

### Q: '%word%' 대신 'word%'는?
**A**: 'word%'는 B-tree 인덱스로 충분. 더 효율적. %가 앞에 붙으면 pg_trgm 필요.

### Q: 얼마나 자주 ANALYZE 실행?
**A**: 자동 autovacuum으로 충분. 수동 ANALYZE는 대량 INSERT 후 1회만.

### Q: 프로덕션에서 인덱스 생성?
**A**: `CREATE INDEX CONCURRENTLY`로 온라인 생성. 다운타임 없음.

### Q: 검색 속도가 여전히 느리면?
**A**: 
1. EXPLAIN ANALYZE로 플랜 확인
2. 테이블 행 수 확인 (1000 이상 필요)
3. 통계 정보 확인 (ANALYZE 재실행)
4. 인덱스 크기 확인 (pg_relation_size)
5. 쿼리 패턴 점검 (함수 사용 여부)
