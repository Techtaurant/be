package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Tag
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 게시물 목록 태그 응답 DTO
 *
 * @property id 태그 ID
 * @property name 태그 이름
 */
@Schema(description = "게시물 목록의 태그 정보")
data class PostListTagResponse(
    @field:Schema(description = "태그 ID")
    val id: UUID,

    @field:Schema(description = "태그 이름")
    val name: String,
) {
    companion object {
        fun from(tag: Tag): PostListTagResponse = PostListTagResponse(
            id = tag.id!!,
            name = tag.name,
        )
    }
}
