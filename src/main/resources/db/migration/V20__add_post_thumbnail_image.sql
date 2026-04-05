ALTER TABLE posts
    ADD COLUMN thumbnail_image UUID;

UPDATE posts p
SET thumbnail_image = a.id
FROM (
    SELECT DISTINCT ON (reference_id)
        reference_id,
        id
    FROM attachments
    WHERE reference_type = 'POST'::attachment_reference_type
      AND status = 'CONFIRMED'::attachment_status
    ORDER BY reference_id, created_at ASC, id ASC
) a
WHERE p.id = a.reference_id
  AND p.thumbnail_image IS NULL;

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_thumbnail_image
        FOREIGN KEY (thumbnail_image) REFERENCES attachments(id) ON DELETE SET NULL;
