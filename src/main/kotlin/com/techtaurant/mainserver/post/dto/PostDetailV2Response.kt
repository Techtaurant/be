package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

@Schema(description = "게시물 상세 응답 v2 - 사용자별 필드 제외")
data class PostDetailV2Response(
    @field:Schema(description = "게시물 ID")
    val id: UUID,
    @field:Schema(description = "게시물 제목")
    val title: String,
    @field:Schema(description = "게시물 본문")
    val content: String,
    @field:Schema(description = "작성자 정보")
    val author: AuthorResponse,
    @field:Schema(description = "카테고리 정보")
    val category: CategoryResponse?,
    @field:Schema(description = "태그 목록")
    val tags: List<PostListTagResponse>,
    @field:Schema(description = "조회수")
    val viewCount: Long,
    @field:Schema(description = "좋아요수")
    val likeCount: Long,
    @field:Schema(description = "댓글수")
    val commentCount: Long,
    @field:Schema(description = "게시물 상태")
    val status: PostStatusEnum,
    @field:Schema(description = "attachmentId와 presigned URL 매핑 목록")
    val attachmentPresignedUrls: List<PostDetailAttachmentPresignedUrlResponse>,
    @field:Schema(description = "작성일")
    val createdAt: Date,
    @field:Schema(description = "수정일")
    val updatedAt: Date,
) {
    companion object {
        fun from(
            post: Post,
            authorProfileImageUrl: String,
            attachmentPresignedUrls: List<PostDetailAttachmentPresignedUrlResponse> = emptyList(),
        ): PostDetailV2Response =
            PostDetailV2Response(
                id = post.id!!,
                title = post.title,
                content = post.content,
                author = AuthorResponse.from(post.author, authorProfileImageUrl),
                category = post.category?.let { CategoryResponse.from(it) },
                tags = post.tags.map { PostListTagResponse.from(it) },
                viewCount = post.viewCount,
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                status = post.status,
                attachmentPresignedUrls = attachmentPresignedUrls,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
    }
}
