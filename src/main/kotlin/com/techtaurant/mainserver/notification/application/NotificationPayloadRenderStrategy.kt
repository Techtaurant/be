package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.entity.NotificationArgument
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import java.util.UUID

interface NotificationPayloadRenderStrategy {
    val type: NotificationType

    fun render(commands: List<NotificationPayloadRenderCommand>): Map<UUID, NotificationPayloadRenderResult>
}

data class NotificationPayloadRenderCommand(
    val notificationId: UUID,
    val recipientUserId: UUID,
    val arguments: List<NotificationArgument>,
)

data class NotificationPayloadRenderResult(
    val payloadHtml: String,
    val arguments: List<NotificationArgument>,
) {
    companion object {
        val EMPTY = NotificationPayloadRenderResult(payloadHtml = "", arguments = emptyList())
    }
}

internal fun List<NotificationArgument>.findTargetId(targetType: NotificationTargetType): UUID? =
    firstOrNull { it.targetType == targetType }?.targetId
