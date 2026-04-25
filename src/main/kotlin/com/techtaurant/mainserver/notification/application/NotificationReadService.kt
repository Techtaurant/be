package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.notification.dto.NotificationCursor
import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationArgumentRepository
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationRecipientRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID

@Service
class NotificationReadService(
    private val notificationRecipientRepository: NotificationRecipientRepository,
    private val notificationArgumentRepository: NotificationArgumentRepository,
    private val notificationPayloadRenderer: NotificationPayloadRenderer,
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
                notificationRecipientRepository.findAllByRecipientUserIdOrderByCreatedAtDescIdDesc(
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

        return CursorPageResponse(
            content = buildNotificationListItems(content),
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    @Transactional
    fun markNotificationsRead(
        userId: UUID,
        notificationIds: List<UUID>,
    ): List<NotificationListItemResponse> {
        val distinctNotificationIds = notificationIds.distinct()
        if (distinctNotificationIds.isEmpty()) {
            return emptyList()
        }

        val unreadRecipients =
            notificationRecipientRepository.findAllByRecipientUserIdAndNotificationIdInAndReadAtIsNull(
                userId = userId,
                notificationIds = distinctNotificationIds,
            )

        if (unreadRecipients.isEmpty()) {
            return emptyList()
        }

        val unreadRecipientsByNotificationId = unreadRecipients.associateBy { it.notification.id!! }
        val updatedRecipients = distinctNotificationIds.mapNotNull(unreadRecipientsByNotificationId::get)
        val readAt = Date()
        updatedRecipients.forEach { recipient ->
            recipient.markAsRead(readAt)
        }

        return buildNotificationListItems(updatedRecipients)
    }

    private fun buildNotificationListItems(recipients: List<NotificationRecipient>): List<NotificationListItemResponse> {
        if (recipients.isEmpty()) {
            return emptyList()
        }

        val notificationIds = recipients.map { it.notification.id!! }
        val argumentsByNotificationId =
            notificationArgumentRepository.findAllByNotificationIdInOrderByCreatedAtAsc(notificationIds)
                .groupBy { it.notification.id!! }
        val renderedPayloadsByNotificationId =
            recipients.groupBy { it.notification.type }
                .flatMap { (type, recipientsByType) ->
                    notificationPayloadRenderer
                        .render(
                            type = type,
                            commands =
                                recipientsByType.map { recipient ->
                                    val notificationId = recipient.notification.id!!
                                    NotificationPayloadRenderCommand(
                                        notificationId = notificationId,
                                        recipientUserId = recipient.recipientUser.id!!,
                                        arguments = argumentsByNotificationId[notificationId].orEmpty(),
                                    )
                                },
                        ).entries
                }.associate { it.key to it.value }

        return recipients.map { recipient ->
            val notification = recipient.notification
            val notificationId = notification.id!!
            val renderedPayload = renderedPayloadsByNotificationId[notificationId] ?: NotificationPayloadRenderResult.EMPTY

            NotificationListItemResponse.from(
                recipient = recipient,
                payloadHtml = renderedPayload.payloadHtml,
                arguments = renderedPayload.arguments,
            )
        }
    }
}
