-- flyway:executeInTransaction=false
-- Link cursor queries sort by created_at_utc and id. Build this index separately
-- from the V37 backfill so Flyway does not mix transactional and non-transactional statements.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_links_created_at_utc_id ON links(created_at_utc DESC, id DESC);
