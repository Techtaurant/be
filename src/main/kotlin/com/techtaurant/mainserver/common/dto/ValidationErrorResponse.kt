package com.techtaurant.mainserver.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "검증 오류 응답 데이터")
data class ValidationErrorResponse(
    @field:Schema(description = "필드별 검증 오류 메시지")
    val errors: Map<String, String>,
)
