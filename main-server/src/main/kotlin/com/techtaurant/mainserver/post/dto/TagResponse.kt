package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.Tag
import java.util.UUID

data class TagResponse(
    val id: UUID,
    val name: String,
    val postCount: Long,
) {
    companion object {
        fun from(tag: Tag, postCount: Long) = TagResponse(
            id = tag.id!!,
            name = tag.name,
            postCount = postCount,
        )
    }
}
