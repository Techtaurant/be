package com.techtaurant.mainserver.notification.infrastructure.out

import com.techtaurant.mainserver.notification.entity.NotificationTarget
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificationTargetRepository : JpaRepository<NotificationTarget, UUID> {
    fun findAllByNotificationIdOrderByCreatedAtAsc(notificationId: UUID): List<NotificationTarget>

    fun findAllByNotificationIdInOrderByCreatedAtAsc(notificationIds: Collection<UUID>): List<NotificationTarget>
}
