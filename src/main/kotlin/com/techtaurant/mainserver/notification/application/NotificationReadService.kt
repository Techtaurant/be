package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.notification.dto.NotificationCursor
import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationRecipientRepository
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationTargetRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID

@Service
class NotificationReadService(
    private val notificationRecipientRepository: NotificationRecipientRepository,
    private val notificationTargetRepository: NotificationTargetRepository,
) {
    @Transactional(readOnly = true)
    fun getMyNotifications(
        userId: UUID,
        cursor: String?,
        size: Int,
    ): CursorPageResponse<NotificationListItemResponse> {
        val notificationCursor = cursor?.let { NotificationCursor.decode(it) }

        if (cursor != null && notificationCursor == null) {
            return CursorPageResponse(
                content = emptyList(),
                nextCursor = null,
                hasNext = false,
                size = 0,
            )
        }

        val recipients =
            if (notificationCursor == null) {
                notificationRecipientRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(
                    userId = userId,
                    pageable = PageRequest.of(0, size + 1),
                )
            } else {
                notificationRecipientRepository.findPageByUserIdAndCursor(
                    userId = userId,
                    cursorCreatedAt = notificationCursor.createdAt,
                    cursorId = notificationCursor.id,
                    pageable = PageRequest.of(0, size + 1),
                )
            }

        val hasNext = recipients.size > size
        val content = recipients.take(size)
        val nextCursor =
            if (hasNext && content.isNotEmpty()) {
                NotificationCursor.from(content.last()).encode()
            } else {
                null
            }

        val notificationIds = content.map { it.notification.id!! }
        val targetsByNotificationId =
            if (notificationIds.isEmpty()) {
                emptyMap()
            } else {
                notificationTargetRepository.findAllByNotificationIdInOrderByCreatedAtAsc(notificationIds)
                    .groupBy { it.notification.id!! }
            }

        return CursorPageResponse(
            content =
                content.map { recipient ->
                    NotificationListItemResponse.from(
                        recipient = recipient,
                        targets = targetsByNotificationId[recipient.notification.id!!].orEmpty(),
                    )
                },
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    @Transactional
    fun markNotificationsRead(
        userId: UUID,
        notificationIds: List<UUID>,
    ) {
        val distinctNotificationIds = notificationIds.distinct()
        if (distinctNotificationIds.isEmpty()) {
            return
        }

        val unreadRecipients =
            notificationRecipientRepository.findAllByUserIdAndNotificationIdInAndReadAtIsNull(
                userId = userId,
                notificationIds = distinctNotificationIds,
            )

        if (unreadRecipients.isEmpty()) {
            return
        }

        val readAt = Date()
        unreadRecipients.forEach { recipient ->
            recipient.markAsRead(readAt)
        }
    }
}
