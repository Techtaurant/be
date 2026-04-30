package com.techtaurant.mainserver.notification.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

@Schema(description = "알림 읽음 처리 요청")
data class MarkNotificationsReadRequest(
    @field:NotEmpty(message = "읽음 처리할 알림 ID는 최소 1개 이상이어야 합니다")
    @field:Schema(description = "읽음 처리할 알림 ID 목록", required = true)
    val notificationIds: List<UUID>,
)
