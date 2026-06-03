package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.user.entity.UserBan
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "사용자 차단 응답")
data class UserBanResponse(
    @field:Schema(description = "차단 대상 사용자 ID")
    val userId: UUID,
    @field:Schema(description = "차단 대상 사용자 이름")
    val name: String,
    @field:Schema(description = "차단 시각")
    val bannedAt: Instant,
) {
    companion object {
        fun from(userBan: UserBan): UserBanResponse =
            UserBanResponse(
                userId = userBan.bannedUser.id!!,
                name = userBan.bannedUser.name,
                bannedAt = userBan.createdAt,
            )
    }
}
