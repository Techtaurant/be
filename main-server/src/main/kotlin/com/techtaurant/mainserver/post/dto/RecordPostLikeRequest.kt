package com.techtaurant.mainserver.post.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * 게시글 좋아요/싫어요 기록 요청 DTO
 * 사용자의 게시글에 대한 평가를 기록합니다.
 *
 * @property isLiked TRUE이면 좋아요, FALSE이면 싫어요를 의미합니다.
 */
@Schema(description = "게시글 좋아요/싫어요 기록 요청")
data class RecordPostLikeRequest(
    @field:NotNull(message = "좋아요 여부는 필수입니다")
    @field:Schema(
        description = "좋아요 여부 (true: 좋아요, false: 싫어요)",
        example = "true",
        required = true,
    )
    val isLiked: Boolean,
)
