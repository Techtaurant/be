package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Tag
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 태그 응답 DTO
 *
 * @property id 태그 ID
 * @property name 태그 이름
 */
@Schema(description = "태그 정보")
data class TagResponse(
    @field:Schema(description = "태그 ID")
    val id: UUID,

    @field:Schema(description = "태그 이름")
    val name: String,

    @field:Schema(description = "태그에 속한 게시물 수")
    val postCount: Long,
) {
    companion object {
        fun from(tag: Tag, postCount: Long): TagResponse = TagResponse(
            id = tag.id!!,
            name = tag.name,
            postCount = postCount,
        )
    }
}
