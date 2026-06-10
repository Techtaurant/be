package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.common.enums.LikeStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

/**
 * 댓글 목록 응답 DTO
 */
@Schema(description = "댓글 목록 응답")
data class CommentListResponse(
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
    @field:Schema(description = "작성자 프로필 이미지 URL")
    val authorProfileImageUrl: String,
    @field:Schema(description = "부모 댓글 ID", nullable = true)
    val parentId: UUID?,
    @field:Schema(description = "댓글 깊이 (0: 댓글, 1: 대댓글)")
    val depth: Int,
    @field:Schema(description = "좋아요 수")
    val likeCount: Long,
    @field:Schema(description = "대댓글 수")
    val replyCount: Long,
    @field:Schema(description = "현재 사용자의 좋아요 상태")
    val likeStatus: LikeStatus,
    @field:Schema(description = "삭제 여부")
    val isDeleted: Boolean,
    @field:Schema(description = "현재 사용자가 차단한 작성자의 댓글인지 여부")
    val isBanned: Boolean,
    @field:Schema(description = "생성 시각")
    val createdAt: Instant,
    @field:Schema(description = "수정 시각")
    val updatedAt: Instant,
) {
    companion object {
        fun from(
            comment: Comment,
            likeStatus: LikeStatus = LikeStatus.NONE,
            authorProfileImageUrl: String,
        ): CommentListResponse {
            return CommentListResponse(
                id = comment.id!!,
                content = comment.content,
                postId = comment.post.id!!,
                authorId = comment.author.id!!,
                authorName = comment.author.name,
                authorProfileImageUrl = authorProfileImageUrl,
                parentId = comment.parent?.id,
                depth = comment.depth,
                likeCount = comment.likeCount,
                replyCount = comment.replyCount,
                likeStatus = likeStatus,
                isDeleted = comment.deletedAt != null,
                isBanned = false,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
        }

        fun fromMasked(
            comment: Comment,
            likeStatus: LikeStatus,
            maskedAuthorId: UUID,
            maskedAuthorName: String,
            maskedAuthorProfileImageUrl: String,
            maskedContent: String,
        ): CommentListResponse {
            return CommentListResponse(
                id = comment.id!!,
                content = maskedContent,
                postId = comment.post.id!!,
                authorId = maskedAuthorId,
                authorName = maskedAuthorName,
                authorProfileImageUrl = maskedAuthorProfileImageUrl,
                parentId = comment.parent?.id,
                depth = comment.depth,
                likeCount = comment.likeCount,
                replyCount = comment.replyCount,
                likeStatus = likeStatus,
                isDeleted = comment.deletedAt != null,
                isBanned = true,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
        }
    }
}
