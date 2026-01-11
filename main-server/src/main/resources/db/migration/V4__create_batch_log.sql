CREATE TABLE batch_log (
    id UUID PRIMARY KEY,
    batch_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    error_message TEXT,
    screenshot_base64 TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_batch_log_batch_name ON batch_log(batch_name);
CREATE INDEX idx_batch_log_status ON batch_log(status);
CREATE INDEX idx_batch_log_started_at ON batch_log(started_at);
