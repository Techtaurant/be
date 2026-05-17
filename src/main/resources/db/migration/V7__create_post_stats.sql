-- posts 테이블에 통계 캐시 컬럼 추가
ALTER TABLE posts
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN stats_updated_at TIMESTAMP;

-- 동기화 대상 조회용 인덱스
CREATE INDEX idx_posts_stats_sync ON posts(stats_updated_at, updated_at);

-- 커서 기반 페이지네이션용 복합 인덱스
CREATE INDEX idx_posts_cursor ON posts(created_at DESC, id DESC);

-- 일별 증분 통계 테이블 (이벤트 로그)
CREATE TABLE post_daily_stats (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, stat_date)
);

CREATE INDEX idx_post_daily_stats_post_date ON post_daily_stats(post_id, stat_date DESC);
CREATE INDEX idx_post_daily_stats_date ON post_daily_stats(stat_date);
