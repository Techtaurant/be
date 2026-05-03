DELETE FROM notification_targets nt
USING notifications n, notification_recipients nr
WHERE nt.notification_id = n.id
  AND nr.notification_id = n.id
  AND n.type = 'FOLLOW'
  AND nt.role = 'TARGET'
  AND nt.target_type = 'USER'
  AND nt.target_id = nr.user_id;

ALTER TABLE notification_targets
    DROP CONSTRAINT uk_notification_targets_notification_id_role_target_type_target_id;

ALTER TABLE notification_targets
    DROP COLUMN role;

DROP TYPE notification_target_role;

ALTER TABLE notification_targets
    RENAME TO notification_arguments;

ALTER INDEX idx_notification_targets_notification_id
    RENAME TO idx_notification_arguments_notification_id;

ALTER INDEX idx_notification_targets_target_type_target_id
    RENAME TO idx_notification_arguments_target_type_target_id;

ALTER TABLE notification_arguments
    ADD CONSTRAINT uk_notification_arguments_notification_id_target_type_target_id
        UNIQUE (notification_id, target_type, target_id);
