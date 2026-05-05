package com.techtaurant.mainserver.link.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "링크 읽음 상태 변경 요청")
data class RecordLinkReadRequest(
    @field:NotNull(message = "읽음 상태는 필수입니다")
    @field:Schema(description = "읽음 상태 (true: 읽음, false: 안읽음)", example = "true")
    val isRead: Boolean,
)
