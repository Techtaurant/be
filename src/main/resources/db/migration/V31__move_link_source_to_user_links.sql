-- 기존 links.source_company_user_id 연결이 있으면 user_links로 이관한 뒤 단일 출처 컬럼을 제거합니다.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'links'
          AND column_name = 'source_company_user_id'
    ) THEN
        INSERT INTO user_links (id, user_id, link_id, created_at, updated_at)
        SELECT
            gen_random_uuid(),
            l.source_company_user_id,
            l.id,
            l.created_at,
            l.updated_at
        FROM links l
        WHERE l.source_company_user_id IS NOT NULL
        ON CONFLICT ON CONSTRAINT uk_user_links_user_id_link_id DO NOTHING;

        DROP INDEX IF EXISTS idx_links_source_company_user_id;

        ALTER TABLE links
            DROP COLUMN source_company_user_id;
    END IF;
END $$;
