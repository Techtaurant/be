-- V5: 블로그 Full Text Search를 위한 인덱스 생성
-- pg_trgm extension을 사용한 한국어 검색 지원 (2글자 이상)

-- pg_trgm extension 활성화 (trigram 기반 유사도 검색)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- name 컬럼에 GIN 인덱스 생성 (trigram ops 사용)
-- 이 인덱스는 LIKE, ILIKE, similarity 연산자를 빠르게 처리
CREATE INDEX idx_blogs_name_trgm ON blogs USING GIN (name gin_trgm_ops);

-- display_name 컬럼에 GIN 인덱스 생성
CREATE INDEX idx_blogs_display_name_trgm ON blogs USING GIN (display_name gin_trgm_ops);

-- 복합 검색을 위한 GIN 인덱스 (name과 display_name 통합 검색)
-- 사용 예시: SELECT * FROM blogs WHERE (name || ' ' || COALESCE(display_name, '')) % '검색어';
CREATE INDEX idx_blogs_combined_trgm ON blogs USING GIN ((name || ' ' || COALESCE(display_name, '')) gin_trgm_ops);

-- 사용법 예시:
-- 1. ILIKE를 사용한 부분 일치 검색 (정확한 부분 문자열)
--    SELECT * FROM blogs WHERE name ILIKE '%검색어%';
--
-- 2. similarity 함수를 사용한 유사도 검색 (오타 허용)
--    SELECT *, similarity(name, '검색어') as sim
--    FROM blogs
--    WHERE name % '검색어'
--    ORDER BY sim DESC;
--
-- 3. word_similarity를 사용한 단어 단위 유사도 검색
--    SELECT *, word_similarity('검색어', name) as sim
--    FROM blogs
--    WHERE '검색어' <% name
--    ORDER BY sim DESC;
--
-- 4. 복합 검색 (name 또는 display_name에서 검색)
--    SELECT * FROM blogs
--    WHERE (name || ' ' || COALESCE(display_name, '')) % '검색어';

-- 성능 최적화를 위한 similarity threshold 설정 (선택적)
-- 기본값은 0.3이며, 0.1~0.3 사이로 설정하면 한국어 2글자도 검색 가능
-- SET pg_trgm.similarity_threshold = 0.2;
-- 이 설정은 세션별로 적용되며, application.yml에서 전역 설정 가능
