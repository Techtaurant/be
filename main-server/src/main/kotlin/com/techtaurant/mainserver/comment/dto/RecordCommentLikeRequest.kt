package com.techtaurant.mainserver.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * 댓글 좋아요/취소 기록 요청 DTO
 * 사용자의 댓글에 대한 좋아요를 기록합니다.
 *
 * @property isLiked TRUE이면 좋아요, FALSE이면 좋아요 취소를 의미합니다.
 */
@Schema(description = "댓글 좋아요/취소 기록 요청")
data class RecordCommentLikeRequest(
    @field:NotNull(message = "좋아요 여부는 필수입니다")
    @field:Schema(
        description = "좋아요 여부 (true: 좋아요, false: 좋아요 취소)",
        example = "true",
        required = true,
    )
    var isLiked: Boolean,
)
