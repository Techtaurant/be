CREATE TABLE article_links (
    id UUID PRIMARY KEY,
    blog_id UUID NOT NULL,
    url TEXT NOT NULL,
    title VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_article_links_blog FOREIGN KEY (blog_id) REFERENCES blogs(id) ON DELETE CASCADE
);

CREATE INDEX idx_article_links_blog_id ON article_links(blog_id);
CREATE INDEX idx_article_links_type ON article_links(type);
CREATE INDEX idx_article_links_deleted_at ON article_links(deleted_at);
