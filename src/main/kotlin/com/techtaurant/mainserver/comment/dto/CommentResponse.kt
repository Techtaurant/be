package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.comment.entity.Comment
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * 댓글 응답 DTO
 */
@Schema(description = "댓글 응답")
data class CommentResponse(
    @field:Schema(description = "댓글 ID")
    val id: UUID,
    @field:Schema(description = "댓글 내용")
    val content: String,
    @field:Schema(description = "게시물 ID")
    val postId: UUID,
    @field:Schema(description = "작성자 ID")
    val authorId: UUID,
    @field:Schema(description = "작성자 이름")
    val authorName: String,
    @field:Schema(description = "부모 댓글 ID", nullable = true)
    val parentId: UUID?,
    @field:Schema(description = "댓글 깊이 (0: 댓글, 1: 대댓글)")
    val depth: Int,
    @field:Schema(description = "삭제 여부")
    val isDeleted: Boolean,
    @field:Schema(description = "생성 시각")
    val createdAt: Instant,
    @field:Schema(description = "수정 시각")
    val updatedAt: Instant,
) {
    companion object {
        private val DELETED_AUTHOR_ID: UUID = UUID(0, 0)
        private const val DELETED_AUTHOR_NAME = "삭제된 사용자"

        fun from(comment: Comment): CommentResponse {
            val author = comment.author
            return CommentResponse(
                id = comment.id!!,
                content = comment.content,
                postId = comment.post.id!!,
                authorId = author?.id ?: DELETED_AUTHOR_ID,
                authorName = author?.name ?: DELETED_AUTHOR_NAME,
                parentId = comment.parent?.id,
                depth = comment.depth,
                isDeleted = comment.deletedAt != null,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
        }
    }
}
