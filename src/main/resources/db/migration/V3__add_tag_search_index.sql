-- pg_trgm 확장 활성화 (LIKE '%keyword%' 검색에 인덱스 사용 가능)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 태그 이름 검색을 위한 GIN 인덱스 (대소문자 무시, 부분 일치)
CREATE INDEX idx_tags_name_trgm ON tags USING GIN (LOWER(name) gin_trgm_ops);
