package com.techtaurant.mainserver.notification.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "안읽은 알림 수 응답")
data class NotificationUnreadCountResponse(
    @field:Schema(description = "현재 로그인한 사용자의 안읽은 알림 수", example = "3")
    val unreadCount: Long,
)
