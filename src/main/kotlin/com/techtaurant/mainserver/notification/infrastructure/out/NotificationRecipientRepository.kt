package com.techtaurant.mainserver.notification.infrastructure.out

import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationRecipientRepository : JpaRepository<NotificationRecipient, UUID> {
    fun findAllByNotificationIdOrderByCreatedAtAsc(notificationId: UUID): List<NotificationRecipient>
}
