-- 게시물 상태 컬럼 추가 (DRAFT/PUBLISHED/PRIVATE)
ALTER TABLE posts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';

-- 기존 데이터를 PUBLISHED 상태로 설정
UPDATE posts SET status = 'PUBLISHED' WHERE status IS NULL;

-- 성능 최적화를 위한 인덱스 생성
CREATE INDEX idx_posts_status ON posts(status);
CREATE INDEX idx_posts_author_status ON posts(author_id, status);

-- 인덱스 설명:
-- idx_posts_status: 공개 게시물 목록 조회 시 사용 (WHERE status = 'PUBLISHED')
-- idx_posts_author_status: 특정 사용자의 DRAFT 목록 조회 시 사용 (WHERE author_id = ? AND status = 'DRAFT')
