-- pg_trgm 확장 활성화 (Trigram 검색)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 사용자 이름에 대한 Trigram GIN 인덱스 생성
-- 이를 통해 LIKE '%검색어%' 쿼리를 매우 빠르게 처리 가능
CREATE INDEX IF NOT EXISTS idx_users_name_trgm
ON users USING GIN (name gin_trgm_ops);
