ALTER TABLE comments
    DROP CONSTRAINT comments_author_id_fkey;

ALTER TABLE comments
    ALTER COLUMN author_id DROP NOT NULL;

ALTER TABLE comments
    ADD CONSTRAINT comments_author_id_fkey
        FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;
