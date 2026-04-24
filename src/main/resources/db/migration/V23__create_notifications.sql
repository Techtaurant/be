CREATE TYPE notification_type AS ENUM (
    'POST_COMMENT',
    'COMMENT_REPLY',
    'FOLLOWER_POST',
    'FOLLOW'
);

CREATE TYPE notification_target_role AS ENUM (
    'ACTOR',
    'TARGET'
);

CREATE TYPE notification_target_type AS ENUM (
    'USER',
    'POST',
    'COMMENT'
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    type notification_type NOT NULL,
    payload_html TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_targets (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    role notification_target_role NOT NULL,
    target_type notification_target_type NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_targets_notification_id_role_target_type_target_id
        UNIQUE (notification_id, role, target_type, target_id)
);

CREATE TABLE notification_recipients (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_recipients_notification_id_user_id UNIQUE (notification_id, user_id)
);

CREATE INDEX idx_notifications_type_created_at ON notifications(type, created_at DESC);
CREATE INDEX idx_notification_targets_notification_id ON notification_targets(notification_id);
CREATE INDEX idx_notification_targets_target_type_target_id ON notification_targets(target_type, target_id);
CREATE INDEX idx_notification_recipients_notification_id ON notification_recipients(notification_id);
CREATE INDEX idx_notification_recipients_user_id_created_at ON notification_recipients(user_id, created_at DESC);
CREATE INDEX idx_notification_recipients_user_id_read_at ON notification_recipients(user_id, read_at);
