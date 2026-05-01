package com.techtaurant.mainserver.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "회사 정보 응답")
data class CompanyResponse(
    @field:Schema(description = "회사 사용자 ID", example = "01234567-89ab-cdef-0123-456789abcdef")
    val id: UUID,
    @field:Schema(description = "회사명", example = "토스")
    val name: String,
    @field:Schema(description = "회사 이메일", example = "contact@toss.im")
    val email: String,
    @field:Schema(description = "회사 프로필 이미지 URL", example = "https://static.toss.im/logo.png")
    val profileImageUrl: String,
) {
    companion object {
        fun from(userResponse: UserResponse): CompanyResponse {
            return CompanyResponse(
                id = userResponse.id,
                name = userResponse.name,
                email = userResponse.email,
                profileImageUrl = userResponse.profileImageUrl,
            )
        }
    }
}
