package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "사용자 정보 응답")
data class UserResponse(
    @field:Schema(description = "사용자 ID", example = "01234567-89ab-cdef-0123-456789abcdef")
    val id: UUID,

    @field:Schema(description = "이름", example = "홍길동")
    val name: String,

    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String,

    @field:Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val profileImageUrl: String,

) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id ?: throw ApiException(UserStatus.ID_NOT_FOUND),
                name = user.name,
                email = user.email,
                profileImageUrl = user.profileImageUrl,
            )
        }
    }
}
