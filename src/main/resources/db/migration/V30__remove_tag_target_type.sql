DROP INDEX IF EXISTS idx_tags_name_target_type;

ALTER TABLE tags
    DROP CONSTRAINT IF EXISTS uk_tags_name_target_type;

ALTER TABLE tags
    DROP COLUMN IF EXISTS target_type;

ALTER TABLE tags
    ADD CONSTRAINT uk_tags_name UNIQUE (name);
