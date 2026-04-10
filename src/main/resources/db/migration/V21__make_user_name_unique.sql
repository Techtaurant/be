WITH duplicated_users AS (
    SELECT
        id,
        name,
        ROW_NUMBER() OVER (
            PARTITION BY name
            ORDER BY created_at ASC, id ASC
        ) AS duplicate_order
    FROM users
),
renamed_users AS (
    SELECT
        id,
        LEFT(name, 245) || '_' || duplicate_order AS unique_name
    FROM duplicated_users
    WHERE duplicate_order > 1
)
UPDATE users u
SET name = renamed_users.unique_name
FROM renamed_users
WHERE u.id = renamed_users.id;

ALTER TABLE users
    ADD CONSTRAINT uk_users_name UNIQUE (name);
