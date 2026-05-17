package com.techtaurant.mainserver.notification.dto

import com.techtaurant.mainserver.notification.entity.NotificationArgument
import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
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
    @field:Schema(description = "알림 썸네일 URL")
    val thumbnailUrl: String,
    @field:Schema(description = "읽음 여부")
    val isRead: Boolean,
    @field:Schema(description = "읽음 시각", nullable = true)
    val readAt: Date?,
    @field:Schema(description = "알림 생성 시각")
    val createdAt: Date,
    @field:Schema(description = "알림 메시지 인자 목록")
    val arguments: List<NotificationArgumentResponse>,
) {
    companion object {
        fun from(
            recipient: NotificationRecipient,
            payloadHtml: String,
            thumbnailUrl: String,
            arguments: List<NotificationArgument>,
        ): NotificationListItemResponse {
            val notification = recipient.notification

            return NotificationListItemResponse(
                id = notification.id!!,
                type = notification.type,
                payloadHtml = payloadHtml,
                thumbnailUrl = thumbnailUrl,
                isRead = recipient.readAt != null,
                readAt = recipient.readAt,
                createdAt = recipient.createdAt,
                arguments =
                    arguments
                        .sortedWith(
                            compareBy<NotificationArgument>(
                                { if (it.targetType == NotificationTargetType.USER) 0 else 1 },
                                { it.createdAt },
                                { it.id },
                            ),
                        ).map(NotificationArgumentResponse::from),
            )
        }
    }
}
