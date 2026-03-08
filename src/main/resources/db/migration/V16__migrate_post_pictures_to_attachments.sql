-- attachments 테이블 생성 (post_pictures를 대체하는 범용 첨부파일 테이블)
CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    reference_id UUID,
    reference_type VARCHAR(50) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 레퍼런스 조회 최적화 인덱스
CREATE INDEX idx_attachments_reference ON attachments(reference_id, reference_type);
-- 상태별 조회 최적화 인덱스
CREATE INDEX idx_attachments_status ON attachments(status);

-- post_pictures 데이터를 attachments로 마이그레이션
-- 기존 picture_url을 object_key로 사용하며 CONFIRMED 상태로 이관
INSERT INTO attachments (
    id,
    reference_id,
    reference_type,
    object_key,
    status,
    original_file_name,
    content_type,
    file_size,
    created_at,
    updated_at
)
SELECT
    id,
    post_id AS reference_id,
    'POST' AS reference_type,
    picture_url AS object_key,
    'CONFIRMED' AS status,
    '' AS original_file_name,
    'image/jpeg' AS content_type,
    0 AS file_size,
    created_at,
    updated_at
FROM post_pictures;

-- 기존 post_pictures 테이블 삭제
DROP TABLE post_pictures;
