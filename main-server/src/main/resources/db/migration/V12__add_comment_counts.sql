-- 댓글에 좋아요 수, 대댓글 수 컬럼 추가
ALTER TABLE comments ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE comments ADD COLUMN reply_count BIGINT NOT NULL DEFAULT 0;

-- 정렬 성능을 위한 인덱스 추가
CREATE INDEX idx_comments_like_count ON comments(like_count);
CREATE INDEX idx_comments_reply_count ON comments(reply_count);
