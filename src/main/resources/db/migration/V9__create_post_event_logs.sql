-- 게시글 조회 이벤트 로그 테이블
-- 각 조회 이벤트를 기록하여 실시간 통계 집계에 사용
CREATE TABLE post_view_log (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL, -- 비회원 조회는 NULL
    ip_address VARCHAR(45), -- IPv6 지원
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 통계 집계를 위한 인덱스 (post_id + created_at으로 빠른 집계)
CREATE INDEX idx_post_view_log_post_created ON post_view_log(post_id, created_at DESC);
-- 특정 기간 조회수 집계용
CREATE INDEX idx_post_view_log_created ON post_view_log(created_at DESC);

-- 게시글 좋아요 이벤트 로그 테이블
-- 좋아요/취소 이벤트를 기록하여 실시간 통계 집계에 사용
CREATE TABLE post_like_log (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_liked BOOLEAN NOT NULL DEFAULT TRUE, -- TRUE: 좋아요, FALSE: 좋아요 취소
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 한 사용자가 같은 게시글에 대해 마지막 액션만 유효
    UNIQUE(post_id, user_id)
);

-- 통계 집계를 위한 인덱스
CREATE INDEX idx_post_like_log_post_created ON post_like_log(post_id, created_at DESC);
-- 특정 기간 좋아요 집계용
CREATE INDEX idx_post_like_log_created ON post_like_log(created_at DESC);

-- 테이블 코멘트
COMMENT ON TABLE post_view_log IS '게시글 조회 이벤트 로그';
COMMENT ON COLUMN post_view_log.user_id IS '조회한 사용자 (비회원은 NULL)';
COMMENT ON COLUMN post_view_log.ip_address IS '조회 IP 주소';

COMMENT ON TABLE post_like_log IS '게시글 좋아요 이벤트 로그';
COMMENT ON COLUMN post_like_log.is_liked IS 'TRUE: 좋아요, FALSE: 좋아요 취소';
