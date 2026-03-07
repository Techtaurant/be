-- 기존 categories 테이블에 user_id 추가
ALTER TABLE categories ADD COLUMN user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE;

-- 기존 path unique 제약 삭제 후 (user_id, path) 복합 유니크 제약 추가
ALTER TABLE categories DROP CONSTRAINT categories_path_key;
ALTER TABLE categories ADD CONSTRAINT categories_user_path_unique UNIQUE (user_id, path);

-- 인덱스 추가
CREATE INDEX idx_categories_user_id ON categories(user_id);
