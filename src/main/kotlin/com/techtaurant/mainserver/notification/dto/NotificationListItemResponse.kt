package com.techtaurant.mainserver.notification.dto

import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import com.techtaurant.mainserver.notification.entity.NotificationTarget
import com.techtaurant.mainserver.notification.enums.NotificationTargetRole
import com.techtaurant.mainserver.notification.enums.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

@Schema(description = "알림 목록 아이템")
data class NotificationListItemResponse(
    @field:Schema(description = "알림 ID")
    val id: UUID,
    @field:Schema(description = "알림 타입", example = "FOLLOW")
    val type: NotificationType,
    @field:Schema(description = "알림 HTML payload")
    val payloadHtml: String,
    @field:Schema(description = "읽음 여부")
    val isRead: Boolean,
    @field:Schema(description = "읽음 시각", nullable = true)
    val readAt: Date?,
    @field:Schema(description = "알림 생성 시각")
    val createdAt: Date,
    @field:Schema(description = "알림 대상 목록")
    val targets: List<NotificationTargetResponse>,
) {
    companion object {
        fun from(
            recipient: NotificationRecipient,
            targets: List<NotificationTarget>,
        ): NotificationListItemResponse {
            val notification = recipient.notification

            return NotificationListItemResponse(
                id = notification.id!!,
                type = notification.type,
                payloadHtml = notification.payloadHtml,
                isRead = recipient.readAt != null,
                readAt = recipient.readAt,
                createdAt = recipient.createdAt,
                targets =
                    targets
                        .sortedWith(
                            compareBy<NotificationTarget>(
                                { if (it.role == NotificationTargetRole.ACTOR) 0 else 1 },
                                { it.createdAt },
                                { it.id },
                            ),
                        ).map(NotificationTargetResponse::from),
            )
        }
    }
}
