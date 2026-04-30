package com.techtaurant.mainserver.notification.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "읽음 처리된 알림 목록 응답")
data class MarkNotificationsReadResponse(
    @field:Schema(description = "이번 요청으로 읽음 처리된 알림 목록")
    val notifications: List<NotificationListItemResponse>,
)
