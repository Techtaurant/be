package com.techtaurant.mainserver.notification.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.notification.enums.NotificationTargetRole
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(
    name = "notification_targets",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_targets_notification_id_role_target_type_target_id",
            columnNames = ["notification_id", "role", "target_type", "target_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_notification_targets_notification_id", columnList = "notification_id"),
        Index(name = "idx_notification_targets_target_type_target_id", columnList = "target_type, target_id"),
    ],
)
class NotificationTarget(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    var notification: Notification,
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_target_role")
    var role: NotificationTargetRole,
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "target_type", nullable = false, columnDefinition = "notification_target_type")
    var targetType: NotificationTargetType,
    @Column(name = "target_id", nullable = false, columnDefinition = "UUID")
    var targetId: UUID,
) : EntityBase()
