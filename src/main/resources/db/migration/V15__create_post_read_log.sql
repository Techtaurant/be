-- 게시물 읽음 표시 기록 테이블
-- 사용자가 명시적으로 게시물을 읽었다고 표시한 기록을 저장
-- 레코드 존재 = 읽음, 레코드 없음 = 안읽음
CREATE TABLE post_read_log (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

-- 목록 조회 시 사용자별 읽음 여부 일괄 확인용 인덱스
CREATE INDEX idx_post_read_log_user_post ON post_read_log(user_id, post_id);

COMMENT ON TABLE post_read_log IS '게시물 읽음 표시 기록';
COMMENT ON COLUMN post_read_log.post_id IS '읽음 표시한 게시물';
COMMENT ON COLUMN post_read_log.user_id IS '읽음 표시한 사용자';
