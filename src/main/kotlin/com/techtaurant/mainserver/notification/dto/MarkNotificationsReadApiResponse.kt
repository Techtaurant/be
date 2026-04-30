package com.techtaurant.mainserver.notification.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 읽음 처리 성공 응답")
data class MarkNotificationsReadApiResponse(
    @field:Schema(description = "HTTP 상태 코드", example = "200")
    val status: Int,
    @field:Schema(description = "읽음 처리 결과 데이터")
    val data: MarkNotificationsReadResponse? = null,
    @field:Schema(description = "응답 메시지", example = "OK")
    val message: String,
)
