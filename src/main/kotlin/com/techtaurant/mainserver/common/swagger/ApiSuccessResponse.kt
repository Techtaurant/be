package com.techtaurant.mainserver.common.swagger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공통 성공 응답")
data class ApiSuccessResponse(
    @field:Schema(description = "HTTP 상태 코드", example = "200")
    val status: Int,
    @field:Schema(description = "응답 데이터", nullable = true)
    val data: Any? = null,
    @field:Schema(description = "응답 메시지", example = "OK")
    val message: String,
)
