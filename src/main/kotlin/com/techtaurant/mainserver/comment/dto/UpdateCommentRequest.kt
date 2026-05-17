package com.techtaurant.mainserver.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * 댓글 수정 요청 DTO
 *
 * @property content 수정할 댓글 내용 (필수)
 */
@Schema(description = "댓글 수정 요청")
data class UpdateCommentRequest(
    @field:NotBlank(message = "댓글 내용은 필수입니다")
    @field:Schema(description = "수정할 댓글 내용", example = "수정된 댓글 내용입니다.")
    val content: String,
)
