# PostgreSQL pg_trgm GIN 인덱스 분석 보고서

## 1. pg_trgm 개요

### 트리그램(Trigram)이란?
- 트리그램: 연속된 3개 문자의 조합
- 예: "hello" → " h", "he", "el", "ll", "lo", "o " (패딩 포함)
- pg_trgm은 이러한 3-글자 조합을 인덱싱하여 문자 매칭 최적화

### pg_trgm 지원 연산자
- `LIKE` / `ILIKE` - 문자 패턴 매칭
- `<->` - 트리그램 거리 (Levenshtein 유사도)
- `%` - 트리그램 유사도 (기본값 0.3)
- `pg_trgm_word_similar()` - 단어 유사도

---

## 2. GIN 인덱스 동작 원리

### GIN (Generalized Inverted Index)의 특성
- **역인덱스 구조**: 각 트리그램이 그것을 포함하는 행들을 가리킴
- **장점**: 복합 데이터(배열, JSON, 텍스트)에 최적
- **단점**: 쓰기(INSERT/UPDATE) 성능 저하, 인덱스 크기 증가

### pg_trgm GIN 인덱스 생성
```sql
-- 기본 GIN 인덱스 생성
CREATE INDEX idx_users_nickname_gin ON users USING GIN (nickname gin_trgm_ops);

-- 또는 GIST (GIST는 내구성이 더 좋음)
CREATE INDEX idx_users_nickname_gist ON users USING GIST (nickname gist_trgm_ops);
```

### GIN 인덱스 검색 원리
1. 검색 문자열을 트리그램으로 분해
2. GIN 인덱스에서 매칭되는 트리그램 찾기
3. 매칭되는 트리그램들의 교집합 취하기
4. 결과 행들을 실제 LIKE 패턴으로 재검증 (재확인 단계)

---

## 3. LIKE '%word%' 패턴에서 인덱스 타는 조건

### ✅ 인덱스를 사용하는 조건
1. **pg_trgm 확장 설치 필요**
   ```sql
   CREATE EXTENSION IF NOT EXISTS pg_trgm;
   ```

2. **GIN 또는 GIST 인덱스 존재**
   ```sql
   CREATE INDEX idx_col_gin ON table_name USING GIN (column_name gin_trgm_ops);
   ```

3. **쿼리가 LIKE/ILIKE 사용**
   ```sql
   SELECT * FROM users WHERE nickname LIKE '%john%';
   SELECT * FROM users WHERE nickname ILIKE '%john%';
   ```

4. **WHERE 절에 직접 조건 명시**
   - ✅ `WHERE nickname LIKE '%word%'` (인덱스 사용 가능)
   - ⚠️ `WHERE nickname LIKE CONCAT('%', variable, '%')` (타입에 따라 다름)
   - ⚠️ 함수 안의 칼럼 (예: `WHERE LOWER(nickname) LIKE '%word%'`) - 함수 인덱스 필요

5. **검색어 길이 충분**
   - 기본적으로 트리그램은 최소 3문자 필요
   - 2글자 이상에서는 부분적으로 인덱스 활용 가능

### ❌ 인덱스를 사용하지 않는 조건
1. **pg_trgm 확장 미설치**
2. **GIN/GIST 인덱스 부재**
3. **LIKE '%' 와일드카드 패턴 시작**
   - ⚠️ '%word%' - 시작이 와일드카드 (인덱스 사용 가능하지만 제한적)
   - ⚠️ '%word' - 시작과 끝 모두 와일드카드
   - ✅ 'word%' - 시작만 고정 (B-tree 인덱스로 충분)

4. **통계 정보 부실 (ANALYZE 미실행)**
5. **work_mem/maintenance_work_mem 부족**
6. **enable_seqscan = on (기본값)**

---

## 4. enable_seqscan 설정과의 관계

### enable_seqscan이란?
```sql
-- 전체 세션 설정
SET enable_seqscan = OFF;  -- Sequential Scan 비활성화

-- 현재 쿼리만 적용
SET enable_seqscan = OFF;
SELECT * FROM users WHERE nickname LIKE '%john%';
RESET enable_seqscan;
```

### 왜 enable_seqscan = OFF로 설정?
- PostgreSQL은 **비용 기반 옵티마이저(Cost-Based Optimizer)**로 동작
- 소규모 테이블에서는 전체 스캔이 더 빠를 수 있음
- 옵티마이저가 인덱스 비용이 높다고 판단하면 Seq Scan 선택
- 옳은 해결책은 아니며, 인덱스 비용 조정이 더 정확함

### ⚠️ enable_seqscan = OFF의 문제점
- 모든 Sequential Scan을 강제로 비활성화
- 범위 쿼리나 높은 선택도 조건에서는 악영향
- 장기적 해결책이 아님

---

## 5. 데이터 크기와 통계 업데이트의 영향

### 옵티마이저가 Seq Scan을 선택하는 경우

#### Case 1: 테이블 크기가 작음
```
- 행 수: < 1000
- 옵티마이저 계산: 
  * Seq Scan 비용 = 행 수 × 1.0 = 1000
  * Index Scan 비용 = 인덱스 탐색 + 행 페치 = 높음
  * 결론: Seq Scan 선택
```

#### Case 2: 통계 정보 부실
```sql
-- 통계 정보 갱신 필수
ANALYZE users;  -- 전체 테이블 통계 갱신
ANALYZE users(nickname);  -- 특정 칼럼만 갱신
```

#### Case 3: 인덱스 비용 설정 최적화
```sql
-- 인덱스 스캔 비용 감소 (기본값 4.0)
ALTER INDEX idx_users_nickname_gin SET (fillfactor = 70);

-- 또는 글로벌 설정 (postgresql.conf)
random_page_cost = 1.0              -- SSD 환경 (기본 4.0, HDD)
index_scan_cost = 0.005             -- 기본값 0.005

-- 관련 설정들
seq_page_cost = 1.0                 -- Sequential 페이지 읽기 비용
effective_cache_size = '4GB'        -- OS 캐시 포함 추정치
```

### 통계 갱신이 중요한 이유
```sql
-- 행 분포 확인
SELECT 
    n_distinct,
    avg_width,
    null_frac
FROM pg_stats
WHERE tablename = 'users' AND attname = 'nickname';

-- 선택도(Selectivity) 계산
-- 옵티마이저는 이 정보로 비용 계산
```

---

## 6. 실제 쿼리 플랜에서 GIN 인덱스 활용

### EXPLAIN 분석 방법

#### Step 1: 현재 쿼리 플랜 확인
```sql
-- 기본 실행 계획
EXPLAIN SELECT * FROM users WHERE nickname LIKE '%john%';

-- 상세 분석
EXPLAIN (ANALYZE, BUFFERS, VERBOSE) 
SELECT * FROM users WHERE nickname LIKE '%john%';
```

#### Step 2: Seq Scan 발생 시 대처

##### 문제: Seq Scan이 나타나는 경우
```
QUERY PLAN
────────────────────────────────────
Seq Scan on users  (cost=0.00..35.50 rows=10 width=32)
  Filter: (nickname LIKE '%john%')
```

**진단:**
- 인덱스가 없거나 비용이 높음
- 통계 정보가 부실할 수 있음

##### 해결책 1: GIN 인덱스 확인
```sql
-- 인덱스 존재 여부 확인
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'users' AND indexname LIKE '%nickname%';

-- 인덱스가 없으면 생성
CREATE INDEX idx_users_nickname_gin ON users USING GIN (nickname gin_trgm_ops);

-- 인덱스 유효성 확인
REINDEX INDEX idx_users_nickname_gin;
```

##### 해결책 2: 통계 갱신
```sql
-- 전체 테이블 통계 갱신
ANALYZE users;

-- 또는 aggressive 옵션 (큰 테이블)
ANALYZE users(nickname);

-- 통계 갱신 후 쿼리 재실행
EXPLAIN SELECT * FROM users WHERE nickname LIKE '%john%';
```

##### 해결책 3: 옵티마이저 비용 조정
```sql
-- 세션 수준 임시 조정
SET enable_seqscan = OFF;
EXPLAIN SELECT * FROM users WHERE nickname LIKE '%john%';
RESET enable_seqscan;

-- 또는 인덱스 비용 조정 (더 정확한 방식)
ALTER INDEX idx_users_nickname_gin 
  SET (fillfactor = 70);
```

#### Step 3: Index Scan이 발생하는 경우 (정상)
```
QUERY PLAN
────────────────────────────────────────
Bitmap Index Scan on idx_users_nickname_gin
  Index Cond: (nickname ~~* '%john%')
→ Bitmap Heap Scan on users
    Recheck Cond: (nickname ~~* '%john%')
```

**의미:**
1. 인덱스 스캔으로 트리그램 매칭 행 찾기
2. Bitmap으로 중복 제거
3. Heap 접근으로 실제 행 검증
4. Recheck: LIKE 패턴으로 최종 확인

---

## 7. 최적화된 쿼리 작성법

### ✅ 권장 방식

#### Case 1: 단순 LIKE 검색
```sql
-- ✅ 좋음: 직접 LIKE
SELECT * FROM users 
WHERE nickname LIKE '%' || ? || '%'
ORDER BY created_at DESC;

-- ✅ 더 좋음: ILIKE (대소문자 무시)
SELECT * FROM users 
WHERE nickname ILIKE '%' || ? || '%'
ORDER BY created_at DESC;
```

#### Case 2: 복합 검색 조건
```sql
-- ✅ 좋음: 인덱스 활용 + 추가 필터
SELECT * FROM users 
WHERE nickname LIKE '%' || ? || '%'
  AND status = 'active'
  AND created_at > NOW() - INTERVAL '30 days'
ORDER BY created_at DESC
LIMIT 100;
```

#### Case 3: 대소문자 무시 검색 (함수 인덱스 필요)
```sql
-- 먼저 함수 인덱스 생성
CREATE INDEX idx_users_nickname_lower_gin 
ON users USING GIN (LOWER(nickname) gin_trgm_ops);

-- 그 후 쿼리
SELECT * FROM users 
WHERE LOWER(nickname) LIKE '%' || LOWER(?) || '%'
ORDER BY created_at DESC;
```

#### Case 4: 검색어 정규화
```sql
-- 공백 제거 + 특수문자 제거
SELECT * FROM users 
WHERE REPLACE(LOWER(nickname), ' ', '') 
      LIKE '%' || REPLACE(LOWER(?), ' ', '') || '%'
ORDER BY created_at DESC;
```

### ❌ 피해야 할 방식

```sql
-- ❌ 나쁨: 함수로 감싸기
SELECT * FROM users 
WHERE CONCAT('%', nickname, '%') LIKE ?;  -- 인덱스 활용 불가

-- ❌ 나쁨: 형변환
SELECT * FROM users 
WHERE CAST(nickname AS VARCHAR) LIKE '%john%';

-- ❌ 나쁨: OR 조건 (개별 인덱스 스캔 반복)
SELECT * FROM users 
WHERE nickname LIKE '%john%' OR nickname LIKE '%jane%'
UNION
SELECT * FROM users 
WHERE email LIKE '%@gmail.com%';
-- 대신 다음 사용:
SELECT * FROM users 
WHERE nickname LIKE '%john%' OR nickname LIKE '%jane%';
```

---

## 8. Spring Boot + JPA 환경에서 최적화

### Hibernate 쿼리 최적화

#### ✅ Native Query 사용 (권장)
```kotlin
@Query(
    nativeQuery = true,
    value = """
        SELECT u.* FROM users u
        WHERE u.nickname LIKE CONCAT('%', :keyword, '%')
        ORDER BY u.created_at DESC
        LIMIT :limit
    """
)
fun searchByNickname(@Param("keyword") keyword: String, 
                     @Param("limit") limit: Int): List<User>
```

#### ⚠️ JPQL with predicate builder
```kotlin
// Criteria API 사용 (복잡하지만 동적 쿼리에 좋음)
val cb = entityManager.criteriaBuilder
val query = cb.createQuery(User::class.java)
val root = query.from(User::class.java)

query.where(
    cb.like(root.get("nickname"), "%$keyword%")
)
query.orderBy(cb.desc(root.get("createdAt")))

entityManager.createQuery(query)
    .setMaxResults(limit)
    .resultList
```

### 데이터베이스 마이그레이션 (Flyway)

#### SQL Migration 파일 생성
```sql
-- V2_1__Add_user_nickname_trgm_index.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_users_nickname_gin 
ON users USING GIN (nickname gin_trgm_ops);

-- 통계 갱신
ANALYZE users;
```

---

## 9. 트러블슈팅 체크리스트

### ❌ 여전히 Seq Scan이 발생하는 경우

```sql
-- 1단계: pg_trgm 확장 확인
SELECT extname FROM pg_extension WHERE extname = 'pg_trgm';
-- 결과 없음 → CREATE EXTENSION pg_trgm;

-- 2단계: 인덱스 존재 확인
SELECT * FROM pg_indexes 
WHERE tablename = 'users' AND indexname LIKE '%nickname%';
-- 결과 없음 → CREATE INDEX 실행

-- 3단계: 인덱스 유효성 확인
SELECT * FROM pg_stat_user_indexes 
WHERE relname = 'idx_users_nickname_gin';
-- idx_scan = 0이거나 NULL → 인덱스 미사용

-- 4단계: 통계 갱신
ANALYZE users(nickname);

-- 5단계: 인덱스 재생성
DROP INDEX IF EXISTS idx_users_nickname_gin;
CREATE INDEX CONCURRENTLY idx_users_nickname_gin 
ON users USING GIN (nickname gin_trgm_ops);

-- 6단계: 쿼리 재테스트
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM users WHERE nickname LIKE '%john%';
```

### 인덱스 크기 확인 및 최적화
```sql
-- 인덱스 크기 확인
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_indexes 
JOIN pg_class ON pg_class.relname = indexname
WHERE tablename = 'users'
ORDER BY pg_relation_size(indexrelid) DESC;

-- 불필요한 인덱스 삭제
DROP INDEX IF EXISTS idx_users_nickname_gin;

-- 인덱스 재구성 (자동으로 최적화)
REINDEX INDEX idx_users_nickname_gin;
```

---

## 10. 성능 벤치마크 예상치

| 조건 | Seq Scan | Index Scan |
|------|----------|-----------|
| 행 수 100 | 0.5ms | 2ms |
| 행 수 10,000 | 5ms | 1ms |
| 행 수 100,000 | 50ms | 2ms |
| 행 수 1,000,000+ | 500ms+ | 5ms+ |

**결론**: 데이터가 충분히 크면 (1만 행 이상) 인덱스가 거의 항상 유리함

---

## 11. 요약 및 권장사항

### 현재 문제 분석
1. Seq Scan 발생 원인:
   - GIN 인덱스 부재 또는 미활용
   - 통계 정보 부실
   - 데이터 크기 작음 (옵티마이저 판단)
   - 옵티마이저 비용 설정 부정확

### 즉각적 조치
1. `CREATE EXTENSION pg_trgm;` (확장 설치)
2. `CREATE INDEX idx_users_nickname_gin ON users USING GIN (nickname gin_trgm_ops);`
3. `ANALYZE users;` (통계 갱신)
4. `EXPLAIN SELECT * FROM users WHERE nickname LIKE '%word%';` (결과 확인)

### 장기 최적화
1. Flyway 마이그레이션으로 인덱스 관리
2. 정기적인 ANALYZE 스케줄링
3. pg_stat_user_indexes 모니터링
4. 쿼리 플랜 로깅 (p6spy 활용)
