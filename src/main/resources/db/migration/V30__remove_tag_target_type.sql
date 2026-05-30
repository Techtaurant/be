-- 매핑 재작성 중 태그/조인 테이블에 동시 쓰기가 들어오지 않도록 마이그레이션 트랜잭션 동안 잠급니다.
LOCK TABLE tags, post_tags, link_tags IN ACCESS EXCLUSIVE MODE;

-- 같은 이름의 POST/LINK 태그 중 가장 먼저 생성된 태그를 대표 태그로 선택합니다.
CREATE TEMP TABLE tag_canonical_mapping ON COMMIT DROP AS
SELECT
    t.id AS tag_id,
    c.canonical_id
FROM tags t
JOIN (
    SELECT DISTINCT ON (name)
        id AS canonical_id,
        name
    FROM tags
    ORDER BY name, created_at, id
) c ON c.name = t.name;

-- 게시물 태그 매핑을 대표 태그 ID로 재작성할 행으로 정규화합니다.
CREATE TEMP TABLE post_tag_canonical_rows ON COMMIT DROP AS
SELECT DISTINCT
    pt.post_id,
    m.canonical_id AS tag_id
FROM post_tags pt
JOIN tag_canonical_mapping m ON m.tag_id = pt.tag_id;

DELETE FROM post_tags;

INSERT INTO post_tags (post_id, tag_id)
SELECT post_id, tag_id
FROM post_tag_canonical_rows;

-- 링크 태그 매핑을 대표 태그 ID로 재작성할 행으로 정규화합니다.
CREATE TEMP TABLE link_tag_canonical_rows ON COMMIT DROP AS
SELECT DISTINCT
    lt.link_id,
    m.canonical_id AS tag_id
FROM link_tags lt
JOIN tag_canonical_mapping m ON m.tag_id = lt.tag_id;

DELETE FROM link_tags;

INSERT INTO link_tags (link_id, tag_id)
SELECT link_id, tag_id
FROM link_tag_canonical_rows;

-- 대표 태그로 매핑이 끝난 중복 태그 행을 제거합니다.
DELETE FROM tags t
USING tag_canonical_mapping m
WHERE t.id = m.tag_id
  AND m.tag_id <> m.canonical_id;

-- 태그 타입 구분을 제거하고 이름 기준 유니크 제약으로 되돌립니다.
DROP INDEX IF EXISTS idx_tags_name_target_type;

ALTER TABLE tags
    DROP CONSTRAINT IF EXISTS uk_tags_name_target_type;

ALTER TABLE tags
    DROP COLUMN IF EXISTS target_type;

ALTER TABLE tags
    ADD CONSTRAINT uk_tags_name UNIQUE (name);
