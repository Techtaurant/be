package com.techtaurant.mainserver.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 팔로워 수/팔로우 수 응답")
data class UserFollowCountResponse(
    @field:Schema(description = "팔로워 수", example = "12")
    val followerCount: Long,
    @field:Schema(description = "팔로우 수", example = "7")
    val followingCount: Long,
)
