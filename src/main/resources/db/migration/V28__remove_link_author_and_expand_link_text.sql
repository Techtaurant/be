ALTER TABLE links
    ALTER COLUMN url TYPE VARCHAR(2048),
    ALTER COLUMN summary TYPE TEXT;

ALTER TABLE links
    DROP COLUMN IF EXISTS author_name;

ALTER TABLE link_crawl_batches
    DROP COLUMN IF EXISTS author_selectors;
