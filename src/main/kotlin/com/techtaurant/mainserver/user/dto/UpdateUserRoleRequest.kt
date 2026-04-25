package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.user.enums.UserRole
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 역할 변경 요청")
data class UpdateUserRoleRequest(
    @field:Schema(description = "변경할 사용자 역할", example = "ADMIN", allowableValues = ["USER", "ADMIN"])
    val role: UserRole,
)
