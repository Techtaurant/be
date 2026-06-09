package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.comment.entity.Comment
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "댓글 공개 콘텐츠 목록 응답")
data class CommentContentListResponse(
    @field:Schema(description = "댓글 ID")
    val id: UUID,
    @field:Schema(description = "댓글 내용")
    val content: String,
    @field:Schema(description = "게시물 ID")
    val postId: UUID,
    @field:Schema(description = "작성자 ID")
    val authorId: UUID,
    @field:Schema(description = "부모 댓글 ID", nullable = true)
    val parentId: UUID?,
    @field:Schema(description = "댓글 깊이 (0: 댓글, 1: 대댓글)")
    val depth: Int,
    @field:Schema(description = "생성 시각")
    val createdAt: Instant,
    @field:Schema(description = "수정 시각")
    val updatedAt: Instant,
) {
    companion object {
        private val DELETED_AUTHOR_ID: UUID = UUID(0, 0)

        fun from(comment: Comment): CommentContentListResponse =
            CommentContentListResponse(
                id = comment.id!!,
                content = comment.content,
                postId = comment.post.id!!,
                authorId = comment.author?.id ?: DELETED_AUTHOR_ID,
                parentId = comment.parent?.id,
                depth = comment.depth,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
    }
}
