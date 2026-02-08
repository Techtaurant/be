package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.common.enums.LikeStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/**
 * 댓글 좋아요 상태 기록 요청 DTO
 * 사용자의 댓글에 대한 좋아요 상태를 기록합니다.
 *
 * @property likeStatus 좋아요 상태 (NONE: 취소, LIKE: 좋아요, DISLIKE: 싫어요)
 */
@Schema(description = "댓글 좋아요 상태 기록 요청")
data class RecordCommentLikeRequest(
    @field:NotNull(message = "좋아요 상태는 필수입니다")
    @field:Schema(
        description = "좋아요 상태 (NONE: 취소, LIKE: 좋아요, DISLIKE: 싫어요)",
        example = "LIKE",
        required = true,
    )
    var likeStatus: LikeStatus,
)
