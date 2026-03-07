-- 댓글 좋아요 이벤트 로그 테이블
-- 좋아요/취소 이벤트를 기록하여 실시간 통계 집계에 사용합니다.
CREATE TABLE comment_like_log (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_liked BOOLEAN NOT NULL DEFAULT TRUE, -- TRUE: 좋아요, FALSE: 좋아요 취소
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 한 사용자가 같은 댓글에 대해 마지막 액션만 유효
    UNIQUE(comment_id, user_id)
);

-- 통계 집계를 위한 인덱스
CREATE INDEX idx_comment_like_log_comment_created ON comment_like_log(comment_id, created_at DESC);
-- 특정 기간 좋아요 집계용
CREATE INDEX idx_comment_like_log_created ON comment_like_log(created_at DESC);

-- 테이블 코멘트
COMMENT ON TABLE comment_like_log IS '댓글 좋아요 이벤트 로그';
COMMENT ON COLUMN comment_like_log.is_liked IS 'TRUE: 좋아요, FALSE: 좋아요 취소';
