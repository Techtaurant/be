package com.techtaurant.mainserver.notification.dto

import com.techtaurant.mainserver.notification.entity.NotificationArgument
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "알림 메시지 인자 정보")
data class NotificationArgumentResponse(
    @field:Schema(description = "알림 인자 타입", example = "USER")
    val targetType: NotificationTargetType,
    @field:Schema(description = "알림 인자 ID")
    val targetId: UUID,
) {
    companion object {
        fun from(argument: NotificationArgument): NotificationArgumentResponse =
            NotificationArgumentResponse(
                targetType = argument.targetType,
                targetId = argument.targetId,
            )
    }
}
