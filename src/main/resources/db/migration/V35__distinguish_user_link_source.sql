ALTER TABLE user_links
    ADD COLUMN is_source BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE user_links
SET is_source = TRUE
FROM users
WHERE users.id = user_links.user_id
  AND users.role = 'COMPANY';

ALTER TABLE user_links
    DROP CONSTRAINT uk_user_links_user_id_link_id;

ALTER TABLE user_links
    ADD CONSTRAINT uk_user_links_user_id_link_id_is_source UNIQUE (user_id, link_id, is_source);

DROP INDEX IF EXISTS idx_user_links_user_link;

CREATE INDEX idx_user_links_user_link ON user_links(user_id, link_id, is_source);
CREATE INDEX idx_user_links_link_source ON user_links(link_id, is_source);
