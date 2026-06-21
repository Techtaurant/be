package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.user.entity.UserToken
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "사용자 토큰 발급 응답")
data class UserTokenResponse(
    @field:Schema(description = "사용자 토큰 ID", example = "01234567-89ab-cdef-0123-456789abcdef")
    val id: UUID,
    @field:Schema(description = "토큰이 연결된 사용자 ID", example = "01234567-89ab-cdef-0123-456789abcdef")
    val userId: UUID,
    @field:Schema(description = "관리자가 식별할 토큰 이름", example = "토스 기술블로그 수집 봇")
    val name: String,
    @field:Schema(description = "발급된 JWT 원문. 이 응답 이후에는 다시 조회할 수 없습니다.")
    val token: String,
    @field:Schema(description = "영구 토큰 여부", example = "true")
    val permanent: Boolean,
    @field:Schema(description = "토큰 생성 시각")
    val createdAt: Instant,
) {
    companion object {
        fun from(
            userToken: UserToken,
            token: String,
        ): UserTokenResponse {
            return UserTokenResponse(
                id = userToken.id ?: throw ApiException(DefaultStatus.SERVER_ERROR, "사용자 토큰 ID가 없습니다"),
                userId = userToken.user.id ?: throw ApiException(DefaultStatus.SERVER_ERROR, "사용자 ID가 없습니다"),
                name = userToken.name,
                token = token,
                permanent = true,
                createdAt = userToken.createdAt,
            )
        }
    }
}
