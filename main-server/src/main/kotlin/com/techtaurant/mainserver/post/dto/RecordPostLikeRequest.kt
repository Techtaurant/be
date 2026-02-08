package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.common.enums.LikeStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * 좋아요 상태 기록 요청 DTO
 * 사용자의 게시글에 대한 평가를 기록합니다.
 *
 * @property likeStatus 좋아요 상태 (NONE: 취소, LIKE: 좋아요, DISLIKE: 싫어요)
 */
@Schema(description = "좋아요 상태 기록 요청")
data class RecordPostLikeRequest(
    @field:NotNull(message = "좋아요 상태는 필수입니다")
    @field:Schema(
        description = "좋아요 상태 (NONE: 취소, LIKE: 좋아요, DISLIKE: 싫어요)",
        example = "LIKE",
        required = true,
    )
    val likeStatus: LikeStatus,
)
