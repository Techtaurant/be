package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "사용자 역할 변경 응답")
data class UpdateUserRoleResponse(
    @field:Schema(description = "역할이 변경된 사용자 ID")
    val userId: UUID,
    @field:Schema(description = "변경된 사용자 역할", example = "ADMIN", allowableValues = ["USER", "ADMIN"])
    val role: UserRole,
) {
    companion object {
        fun from(user: User): UpdateUserRoleResponse =
            UpdateUserRoleResponse(
                userId = user.id ?: throw ApiException(UserStatus.ID_NOT_FOUND),
                role = user.role,
            )
    }
}
