package com.techtaurant.mainserver.post.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * 읽음 상태 변경 요청 DTO
 * 사용자가 게시물을 읽음/안읽음으로 표시할 때 사용합니다.
 *
 * @property isRead 읽음 상태 (true: 읽음 표시, false: 안읽음 표시)
 */
@Schema(description = "읽음 상태 변경 요청")
data class RecordPostReadRequest(
    @field:NotNull(message = "읽음 상태는 필수입니다")
    @field:Schema(
        description = "읽음 상태 (true: 읽음 표시, false: 안읽음 표시)",
        example = "true",
        required = true,
    )
    val isRead: Boolean,
)
