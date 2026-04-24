package com.techtaurant.mainserver.notification.dto

import com.techtaurant.mainserver.notification.entity.NotificationTarget
import com.techtaurant.mainserver.notification.enums.NotificationTargetRole
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "알림 대상 정보")
data class NotificationTargetResponse(
    @field:Schema(description = "알림 대상 역할", example = "ACTOR")
    val role: NotificationTargetRole,
    @field:Schema(description = "알림 대상 타입", example = "USER")
    val targetType: NotificationTargetType,
    @field:Schema(description = "알림 대상 ID")
    val targetId: UUID,
) {
    companion object {
        fun from(target: NotificationTarget): NotificationTargetResponse =
            NotificationTargetResponse(
                role = target.role,
                targetType = target.targetType,
                targetId = target.targetId,
            )
    }
}
