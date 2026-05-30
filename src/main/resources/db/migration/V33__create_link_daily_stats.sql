CREATE TABLE link_daily_stats (
    id UUID PRIMARY KEY,
    link_id UUID NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    stat_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    save_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_link_daily_stats_link_id_stat_date UNIQUE (link_id, stat_date)
);

CREATE INDEX idx_link_daily_stats_link_date ON link_daily_stats(link_id, stat_date DESC);
CREATE INDEX idx_link_daily_stats_date ON link_daily_stats(stat_date);
