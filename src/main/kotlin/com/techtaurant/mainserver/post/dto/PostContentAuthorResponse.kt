package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "게시물 콘텐츠 작성자 응답")
data class PostContentAuthorResponse(
    @field:Schema(description = "작성자 ID")
    val id: UUID,
) {
    companion object {
        fun from(user: User): PostContentAuthorResponse =
            PostContentAuthorResponse(
                id = user.id!!,
            )
    }
}
