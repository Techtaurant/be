package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

@Schema(description = "게시물 목록 아이템")
data class PostListItemResponse(
    @field:Schema(description = "게시물 ID")
    val id: UUID,
    @field:Schema(description = "게시물 제목")
    val title: String,
    @field:Schema(description = "작성자 ID")
    val authorId: UUID,
    @field:Schema(description = "작성자 이름")
    val authorName: String,
    @field:Schema(description = "작성자 프로필 이미지 URL")
    val authorProfileImageUrl: String,
    @field:Schema(description = "게시물 썸네일 이미지 URL")
    val thumbnailUrl: String,
    @field:Schema(description = "로그인한 사용자가 읽은 게시물인지 여부")
    val isRead: Boolean,
    @field:Schema(description = "태그 목록")
    val tags: List<PostListTagResponse>,
    @field:Schema(description = "조회수")
    val viewCount: Long,
    @field:Schema(description = "좋아요수")
    val likeCount: Long,
    @field:Schema(description = "댓글수")
    val commentCount: Long,
    @field:Schema(description = "게시물 상태 (DRAFT: 임시저장, PUBLISHED: 발행, PRIVATE: 비공개)")
    val status: PostStatusEnum,
    @field:Schema(description = "작성일")
    val createdAt: Date,
) {
    companion object {
        /**
         * @deprecated PostListReadService.convertToResponse()를 사용하세요.
         * 이 메서드는 썸네일과 읽음 여부를 처리하지 않습니다.
         */
        @Deprecated(
            message = "Use PostListReadService.convertToResponse() instead",
            level = DeprecationLevel.WARNING,
        )
        fun from(post: Post): PostListItemResponse =
            PostListItemResponse(
                id = post.id!!,
                title = post.title,
                authorId = post.author.id!!,
                authorName = post.author.name,
                authorProfileImageUrl = post.author.profileImageUrl,
                thumbnailUrl = "",
                isRead = false,
                tags = post.tags.map { PostListTagResponse.from(it) },
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                status = post.status,
                createdAt = post.createdAt,
            )
    }
}
