package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Post
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

@Schema(description = "게시물 정적 콘텐츠 상세 응답")
data class PostContentDetailResponse(
    @field:Schema(description = "게시물 ID")
    val id: UUID,
    @field:Schema(description = "게시물 제목")
    val title: String,
    @field:Schema(description = "게시물 본문")
    val content: String,
    @field:Schema(description = "작성자 정보")
    val author: PostContentAuthorResponse,
    @field:Schema(description = "카테고리 정보")
    val category: PostContentCategoryResponse?,
    @field:Schema(description = "태그 목록")
    val tags: List<PostListTagResponse>,
    @field:Schema(description = "작성일")
    val createdAt: Date,
    @field:Schema(description = "수정일")
    val updatedAt: Date,
) {
    companion object {
        fun from(post: Post): PostContentDetailResponse =
            PostContentDetailResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                author = PostContentAuthorResponse.from(post.author),
                category = post.category?.let(PostContentCategoryResponse::from),
                tags = post.tags.map { PostListTagResponse.from(it) },
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
    }
}
