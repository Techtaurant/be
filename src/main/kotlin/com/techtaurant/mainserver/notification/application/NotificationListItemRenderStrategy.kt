package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.entity.NotificationArgument
import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import java.util.UUID

interface NotificationListItemRenderStrategy {
    val type: NotificationType

    fun render(commands: List<NotificationListItemRenderCommand>): Map<UUID, NotificationListItemResponse>
}

data class NotificationListItemRenderCommand(
    val recipient: NotificationRecipient,
    val arguments: List<NotificationArgument>,
)

internal fun List<NotificationArgument>.findTargetId(targetType: NotificationTargetType): UUID? =
    firstOrNull { it.targetType == targetType }?.targetId
