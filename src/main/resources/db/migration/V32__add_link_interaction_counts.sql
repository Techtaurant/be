ALTER TABLE links
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE link_view_log (
    id UUID PRIMARY KEY,
    link_id UUID NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_link_view_log_link_created ON link_view_log(link_id, created_at DESC);
CREATE INDEX idx_link_view_log_created ON link_view_log(created_at DESC);

CREATE TABLE link_like_log (
    id UUID PRIMARY KEY,
    link_id UUID NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_liked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_link_like_log_link_id_user_id UNIQUE (link_id, user_id)
);

CREATE INDEX idx_link_like_log_link_created ON link_like_log(link_id, created_at DESC);
CREATE INDEX idx_link_like_log_created ON link_like_log(created_at DESC);

COMMENT ON TABLE link_view_log IS '링크 조회 이벤트 로그';
COMMENT ON COLUMN link_view_log.user_id IS '조회한 사용자 (비회원은 NULL)';
COMMENT ON COLUMN link_view_log.ip_address IS '조회 IP 주소';

COMMENT ON TABLE link_like_log IS '링크 좋아요 이벤트 로그';
COMMENT ON COLUMN link_like_log.is_liked IS 'TRUE: 좋아요, FALSE: 좋아요 취소';
