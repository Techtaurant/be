ALTER TYPE attachment_reference_type ADD VALUE IF NOT EXISTS 'USER';

ALTER TABLE users
    ADD COLUMN service_profile_image_attachment_id UUID;

ALTER TABLE users
    ADD CONSTRAINT fk_users_service_profile_image_attachment
        FOREIGN KEY (service_profile_image_attachment_id) REFERENCES attachments(id) ON DELETE SET NULL;
