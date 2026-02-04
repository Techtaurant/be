-- 게시물 사진 테이블
CREATE TABLE post_pictures (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    picture_url VARCHAR(500) NOT NULL,
    is_thumbnail BOOLEAN NOT NULL DEFAULT false,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 게시물별 사진 조회를 위한 인덱스
CREATE INDEX idx_post_pictures_post_id ON post_pictures(post_id);

-- 썸네일 조회 최적화를 위한 복합 인덱스
CREATE INDEX idx_post_pictures_thumbnail ON post_pictures(post_id, is_thumbnail);
