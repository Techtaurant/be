-- flyway:executeInTransaction=false
-- Link ordering now uses links.created_at_utc. Preserve previously crawled publication dates
-- by moving them into created_at_utc before the application stops reading published_at_utc.

UPDATE links
SET created_at_utc = COALESCE(published_at_utc, published_at AT TIME ZONE 'UTC', created_at_utc, created_at AT TIME ZONE 'UTC')
WHERE published_at_utc IS NOT NULL
   OR published_at IS NOT NULL
   OR created_at_utc IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_links_created_at_utc_id ON links(created_at_utc DESC, id DESC);
