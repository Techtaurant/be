package com.techtaurant.mainserver.notification.entity

import com.techtaurant.mainserver.common.base.EntityBase
import com.techtaurant.mainserver.notification.enums.NotificationType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "notifications")
class Notification(
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_type")
    var type: NotificationType,
    @OneToMany(mappedBy = "notification", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var arguments: MutableList<NotificationArgument> = mutableListOf(),
    @OneToMany(mappedBy = "notification", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var recipients: MutableList<NotificationRecipient> = mutableListOf(),
) : EntityBase() {
    fun addArgument(argument: NotificationArgument) {
        arguments.add(argument)
    }

    fun addRecipient(recipient: NotificationRecipient) {
        recipients.add(recipient)
    }
}
