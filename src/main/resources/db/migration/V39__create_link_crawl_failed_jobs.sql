CREATE TABLE link_crawl_failed_jobs (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES link_crawl_batches(id) ON DELETE CASCADE,
    source_page INTEGER NOT NULL,
    source_page_url VARCHAR(2048) NOT NULL,
    article_url VARCHAR(2048) NOT NULL,
    title VARCHAR(200),
    summary TEXT,
    error_status_code INTEGER NOT NULL,
    error_message TEXT NOT NULL,
    failure_count INTEGER NOT NULL DEFAULT 1,
    last_failed_at_utc TIMESTAMPTZ NOT NULL,
    created_at_utc TIMESTAMPTZ NOT NULL,
    updated_at_utc TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_link_crawl_failed_jobs_batch_article_url UNIQUE (batch_id, article_url)
);

CREATE INDEX idx_link_crawl_failed_jobs_batch_id ON link_crawl_failed_jobs(batch_id);
CREATE INDEX idx_link_crawl_failed_jobs_created_at ON link_crawl_failed_jobs(created_at_utc);
