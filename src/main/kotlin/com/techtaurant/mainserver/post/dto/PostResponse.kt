package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

/**
 * 게시물 응답 DTO
 */
@Schema(description = "게시물 응답")
data class PostResponse(
    @field:Schema(description = "게시물 ID", example = "01234567-89ab-cdef-0123-456789abcdef")
    val id: UUID,
    @field:Schema(description = "제목", example = "Spring Boot 시작하기")
    val title: String,
    @field:Schema(description = "본문", example = "Spring Boot를 사용하면...")
    val content: String,
    @field:Schema(description = "작성자 ID", example = "01234567-89ab-cdef-0123-456789abcdef")
    val authorId: UUID,
    @field:Schema(description = "작성자 이름", example = "홍길동")
    val authorName: String,
    @field:Schema(description = "카테고리 경로", example = "java/spring/deepdive")
    val categoryPath: String?,
    @field:Schema(description = "태그 목록", example = "[\"spring\", \"backend\"]")
    val tags: List<String>,
    @field:Schema(description = "생성일시")
    val createdAt: Date,
    @field:Schema(description = "수정일시")
    val updatedAt: Date,
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id ?: throw ApiException(PostStatus.POST_NOT_FOUND),
                title = post.title,
                content = post.content,
                authorId = post.author.id ?: throw ApiException(PostStatus.POST_NOT_FOUND),
                authorName = post.author.name,
                categoryPath = post.category?.path,
                tags = post.tags.map { it.name },
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
        }
    }
}
