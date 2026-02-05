package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

/**
 * 게시물 상세 조회 응답 DTO
 *
 * @property id 게시물 ID
 * @property title 게시물 제목
 * @property content 게시물 본문
 * @property author 작성자 정보
 * @property category 카테고리 정보
 * @property tags 태그 목록
 * @property viewCount 조회수
 * @property likeCount 좋아요수
 * @property commentCount 댓글수
 * @property isLiked 현재 사용자의 좋아요 여부
 * @property createdAt 작성일
 * @property updatedAt 수정일
 */
@Schema(description = "게시물 상세 응답")
data class PostDetailResponse(
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

    @field:Schema(description = "현재 사용자의 좋아요 여부")
    val isLiked: Boolean,

    @field:Schema(description = "작성일")
    val createdAt: Date,

    @field:Schema(description = "수정일")
    val updatedAt: Date,
) {
    companion object {
        /**
         * Post 엔티티를 PostDetailResponse로 변환합니다.
         *
         * @param post 게시물 엔티티
         * @param isLiked 현재 사용자의 좋아요 여부
         * @return 게시물 상세 응답 DTO
         */
        fun from(post: Post, isLiked: Boolean): PostDetailResponse = PostDetailResponse(
            id = post.id!!,
            title = post.title,
            content = post.content,
            author = AuthorResponse.from(post.author),
            category = post.category?.let { CategoryResponse.from(it) },
            tags = post.tags.map { PostListTagResponse.from(it) },
            viewCount = post.viewCount,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            isLiked = isLiked,
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
        )
    }
}

/**
 * 작성자 정보 응답 DTO
 *
 * @property id 작성자 ID
 * @property name 작성자 이름
 * @property profileImageUrl 프로필 이미지 URL
 */
@Schema(description = "작성자 정보")
data class AuthorResponse(
    @field:Schema(description = "작성자 ID")
    val id: UUID,

    @field:Schema(description = "작성자 이름")
    val name: String,

    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String,
) {
    companion object {
        fun from(user: User): AuthorResponse = AuthorResponse(
            id = user.id!!,
            name = user.name,
            profileImageUrl = user.profileImageUrl,
        )
    }
}
