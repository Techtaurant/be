package com.techtaurant.mainserver.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

/**
 * 댓글 생성 요청 DTO
 *
 * @property content 댓글 내용 (필수)
 * @property postId 게시물 ID (필수)
 * @property parentId 부모 댓글 ID (선택, 대댓글인 경우에만 값을 가짐)
 */
@Schema(description = "댓글 생성 요청")
data class CreateCommentRequest(
    @field:NotBlank(message = "댓글 내용은 필수입니다")
    @field:Schema(description = "댓글 내용", example = "좋은 글이네요!")
    val content: String,
    @field:NotNull(message = "게시물 ID는 필수입니다")
    @field:Schema(description = "게시물 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val postId: UUID,
    @field:Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "550e8400-e29b-41d4-a716-446655440001", nullable = true)
    val parentId: UUID? = null,
)
