package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.user.entity.UserFollow
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "사용자 팔로워/팔로잉 목록 아이템 응답")
data class UserFollowListItemResponse(
    @field:Schema(description = "사용자 ID")
    val userId: UUID,
    @field:Schema(description = "사용자 이름")
    val name: String,
    @field:Schema(description = "사용자 프로필 이미지 URL")
    val profileImageUrl: String,
    @field:Schema(description = "팔로우 생성 시각")
    val followedAt: Instant,
) {
    companion object {
        fun fromFollowing(
            userFollow: UserFollow,
            profileImageUrl: String,
        ): UserFollowListItemResponse =
            UserFollowListItemResponse(
                userId = userFollow.following.id!!,
                name = userFollow.following.name,
                profileImageUrl = profileImageUrl,
                followedAt = userFollow.createdAt,
            )

        fun fromFollower(
            userFollow: UserFollow,
            profileImageUrl: String,
        ): UserFollowListItemResponse =
            UserFollowListItemResponse(
                userId = userFollow.follower.id!!,
                name = userFollow.follower.name,
                profileImageUrl = profileImageUrl,
                followedAt = userFollow.createdAt,
            )
    }
}
