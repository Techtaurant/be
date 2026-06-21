package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.user.entity.UserFollow
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "사용자 팔로우 응답")
data class UserFollowResponse(
    @field:Schema(description = "팔로우한 사용자 ID")
    val userId: UUID,
    @field:Schema(description = "팔로우한 사용자 이름")
    val name: String,
    @field:Schema(description = "팔로우 시각")
    val followedAt: Instant,
) {
    companion object {
        fun from(userFollow: UserFollow): UserFollowResponse =
            UserFollowResponse(
                userId = userFollow.following.id!!,
                name = userFollow.following.name,
                followedAt = userFollow.createdAt,
            )
    }
}
