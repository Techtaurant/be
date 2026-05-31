ALTER TABLE user_links
    ADD COLUMN is_source BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE user_links
SET is_source = TRUE
FROM links, users
WHERE links.id = user_links.link_id
  AND users.id = user_links.user_id
  AND users.role = 'COMPANY'
  AND (
    (
      user_links.created_at = links.created_at
      AND user_links.updated_at = links.updated_at
    )
    OR EXISTS (
      SELECT 1
      FROM link_crawl_batches link_crawl_batch
      WHERE link_crawl_batch.company_user_id = user_links.user_id
        AND LEFT(links.url, LENGTH(link_crawl_batch.base_url)) = link_crawl_batch.base_url
    )
  );

ALTER TABLE user_links
    DROP CONSTRAINT uk_user_links_user_id_link_id;

ALTER TABLE user_links
    ADD CONSTRAINT uk_user_links_user_id_link_id_is_source UNIQUE (user_id, link_id, is_source);

DROP INDEX IF EXISTS idx_user_links_user_link;

CREATE INDEX idx_user_links_user_link ON user_links(user_id, link_id, is_source);
CREATE INDEX idx_user_links_link_source ON user_links(link_id, is_source);
