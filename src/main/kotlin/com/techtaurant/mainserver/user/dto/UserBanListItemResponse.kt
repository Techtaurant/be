package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.user.entity.UserBan
import io.swagger.v3.oas.annotations.media.Schema
import java.util.Date
import java.util.UUID

@Schema(description = "차단한 사용자 목록 아이템 응답")
data class UserBanListItemResponse(
    @field:Schema(description = "차단 대상 사용자 ID")
    val userId: UUID,
    @field:Schema(description = "차단 대상 사용자 이름")
    val name: String,
    @field:Schema(description = "차단 대상 사용자 프로필 이미지 URL", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "차단 시각")
    val bannedAt: Date,
) {
    companion object {
        fun from(
            userBan: UserBan,
            profileImageUrl: String,
        ): UserBanListItemResponse =
            UserBanListItemResponse(
                userId = userBan.bannedUser.id!!,
                name = userBan.bannedUser.name,
                profileImageUrl = profileImageUrl,
                bannedAt = userBan.createdAt,
            )
    }
}
