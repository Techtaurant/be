package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.comment.entity.Comment
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "댓글 공개 메타데이터 응답")
data class CommentMetadataResponse(
    @field:Schema(description = "댓글 ID")
    val commentId: UUID,
    @field:Schema(description = "좋아요 수")
    val likeCount: Long,
    @field:Schema(description = "삭제 여부")
    val isDeleted: Boolean,
) {
    companion object {
        fun from(comment: Comment): CommentMetadataResponse =
            CommentMetadataResponse(
                commentId = comment.id!!,
                likeCount = comment.likeCount,
                isDeleted = comment.deletedAt != null,
            )
    }
}
