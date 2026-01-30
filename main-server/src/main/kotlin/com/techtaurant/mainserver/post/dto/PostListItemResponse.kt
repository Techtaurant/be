package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Post
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

/**
 * 게시물 목록 아이템 응답 DTO
 *
 * @property id 게시물 ID
 * @property title 게시물 제목
 * @property authorName 작성자 이름
 * @property tags 태그 목록
 * @property viewCount 조회수
 * @property likeCount 좋아요수
 * @property commentCount 댓글수
 * @property createdAt 작성일
 */
@Schema(description = "게시물 목록 아이템")
data class PostListItemResponse(
    @field:Schema(description = "게시물 ID")
    val id: UUID,

    @field:Schema(description = "게시물 제목")
    val title: String,

    @field:Schema(description = "작성자 이름")
    val authorName: String,

    @field:Schema(description = "태그 목록")
    val tags: List<PostListTagResponse>,

    @field:Schema(description = "조회수")
    val viewCount: Long,

    @field:Schema(description = "좋아요수")
    val likeCount: Long,

    @field:Schema(description = "댓글수")
    val commentCount: Long,

    @field:Schema(description = "작성일")
    val createdAt: Date,
) {
    companion object {
        fun from(post: Post): PostListItemResponse = PostListItemResponse(
            id = post.id!!,
            title = post.title,
            authorName = post.author.name,
            tags = post.tags.map { PostListTagResponse.from(it) },
            viewCount = post.viewCount,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            createdAt = post.createdAt,
        )
    }
}
