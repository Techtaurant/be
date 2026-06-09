package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Post
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "게시물 정적 콘텐츠 목록 아이템")
data class PostContentListItemResponse(
    @field:Schema(description = "게시물 ID")
    val id: UUID,
    @field:Schema(description = "게시물 제목")
    val title: String,
    @field:Schema(description = "게시물 내용 일부 (최대 2000자)")
    val content: String,
    @field:Schema(description = "작성자 ID")
    val authorId: UUID,
    @field:Schema(description = "게시물 카테고리")
    val category: PostContentCategoryResponse?,
    @field:Schema(description = "태그 목록")
    val tags: List<PostListTagResponse>,
    @field:Schema(description = "작성일")
    val createdAt: Instant,
    @field:Schema(description = "최종 수정일")
    val updatedAt: Instant,
) {
    companion object {
        private const val CONTENT_MAX_LENGTH = 2000

        fun from(post: Post): PostContentListItemResponse =
            PostContentListItemResponse(
                id = post.id!!,
                title = post.title,
                content = post.content.take(CONTENT_MAX_LENGTH),
                authorId = post.author.id!!,
                category = post.category?.let(PostContentCategoryResponse::from),
                tags = post.tags.map { PostListTagResponse.from(it) },
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
    }
}
