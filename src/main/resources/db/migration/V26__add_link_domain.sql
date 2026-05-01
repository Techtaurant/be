ALTER TABLE tags
    ADD COLUMN target_type VARCHAR(20) NOT NULL DEFAULT 'POST';

ALTER TABLE tags
    DROP CONSTRAINT IF EXISTS tags_name_key;

ALTER TABLE tags
    ADD CONSTRAINT uk_tags_name_target_type UNIQUE (name, target_type);

CREATE INDEX idx_tags_name_target_type ON tags(name, target_type);

CREATE TABLE links (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    summary VARCHAR(80) NOT NULL,
    source_company_user_id UUID NOT NULL REFERENCES users(id),
    author_name VARCHAR(255),
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_links_url UNIQUE (url)
);

CREATE INDEX idx_links_source_company_user_id ON links(source_company_user_id);
CREATE INDEX idx_links_published_at ON links(published_at DESC);

CREATE TABLE user_links (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    link_id UUID NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_links_user_id_link_id UNIQUE (user_id, link_id)
);

CREATE INDEX idx_user_links_user_link ON user_links(user_id, link_id);

CREATE TABLE link_read_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    link_id UUID NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_link_read_log_user_id_link_id UNIQUE (user_id, link_id)
);

CREATE INDEX idx_link_read_log_user_link ON link_read_log(user_id, link_id);

CREATE TABLE link_tags (
    link_id UUID NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (link_id, tag_id)
);

CREATE INDEX idx_link_tags_link_id ON link_tags(link_id);
CREATE INDEX idx_link_tags_tag_id ON link_tags(tag_id);

CREATE TABLE link_crawl_batches (
    id UUID PRIMARY KEY,
    company_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    page_uri_template VARCHAR(500) NOT NULL,
    item_selector VARCHAR(500) NOT NULL,
    article_link_selector VARCHAR(500) NOT NULL,
    title_selector VARCHAR(500) NOT NULL,
    summary_selector VARCHAR(500),
    author_selectors TEXT,
    published_at_selectors TEXT,
    tag_names TEXT,
    cron_expression VARCHAR(120) NOT NULL,
    start_page INTEGER NOT NULL DEFAULT 1,
    end_page INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_link_crawl_batches_company_user_id ON link_crawl_batches(company_user_id);
CREATE INDEX idx_link_crawl_batches_active ON link_crawl_batches(active);
