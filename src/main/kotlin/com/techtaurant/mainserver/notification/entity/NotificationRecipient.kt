package com.techtaurant.mainserver.notification.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "notification_recipients",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_recipients_notification_id_user_id",
            columnNames = ["notification_id", "user_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_notification_recipients_notification_id", columnList = "notification_id"),
        Index(name = "idx_notification_recipients_user_id_created_at_utc", columnList = "user_id, created_at_utc"),
        Index(name = "idx_notification_recipients_user_id_read_at_utc", columnList = "user_id, read_at_utc"),
    ],
)
class NotificationRecipient(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    var notification: Notification,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var recipientUser: User,
    @Column(name = "read_at_utc")
    var readAt: Instant? = null,
) : EntityBase() {
    fun markAsRead(readAt: Instant) {
        if (this.readAt == null) {
            this.readAt = readAt
        }
    }
}
