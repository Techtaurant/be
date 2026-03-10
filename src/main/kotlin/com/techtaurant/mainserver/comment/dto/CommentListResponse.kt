package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.common.enums.LikeStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

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
    @field:Schema(description = "작성자 프로필 이미지 URL", nullable = true)
    val authorProfileImageUrl: String?,
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
    @field:Schema(description = "현재 사용자가 차단한 작성자의 댓글인지 여부")
    val isBanned: Boolean,
    @field:Schema(description = "생성 시각")
    val createdAt: Date,
    @field:Schema(description = "수정 시각")
    val updatedAt: Date,
) {
    companion object {
        fun from(
            comment: Comment,
            likeStatus: LikeStatus = LikeStatus.NONE,
            isBanned: Boolean = false,
            authorId: UUID = comment.author.id!!,
            authorName: String = comment.author.name,
            authorProfileImageUrl: String? = comment.author.profileImageUrl,
            content: String = comment.content,
        ): CommentListResponse {
            return CommentListResponse(
                id = comment.id!!,
                content = content,
                postId = comment.post.id!!,
                authorId = authorId,
                authorName = authorName,
                authorProfileImageUrl = authorProfileImageUrl,
                parentId = comment.parent?.id,
                depth = comment.depth,
                likeCount = comment.likeCount,
                replyCount = comment.replyCount,
                likeStatus = likeStatus,
                isBanned = isBanned,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
            )
        }
    }
}
