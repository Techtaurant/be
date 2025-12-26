CREATE TABLE blogs (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200),
    icon_url VARCHAR(500),
    base_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_blogs_name ON blogs(name);
CREATE INDEX idx_blogs_deleted_at ON blogs(deleted_at);
