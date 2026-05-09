package com.techtaurant.mainserver.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "사용자 프로필 이미지 응답")
data class UserProfileImageResponse(
    @field:Schema(description = "사용자 ID")
    val userId: UUID,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String,
)
