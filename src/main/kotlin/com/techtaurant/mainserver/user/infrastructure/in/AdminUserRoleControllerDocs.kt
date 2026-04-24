package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.dto.UpdateUserRoleRequest
import com.techtaurant.mainserver.user.dto.UpdateUserRoleResponse
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "관리자 사용자", description = "관리자 전용 사용자 관리 API")
interface AdminUserRoleControllerDocs {
    @Operation(summary = "사용자 역할 변경", description = "ADMIN 권한을 가진 사용자가 특정 사용자의 역할을 변경합니다")
    @SwaggerApiResponse(
        responseCode = "200",
        description = "역할 변경 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED", "ACCESS_DENIED"]),
            ApiErrorCodeResponse(UserStatus::class, ["USER_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun updateUserRole(
        @Parameter(description = "역할을 변경할 대상 사용자 ID") targetUserId: UUID,
        @Valid request: UpdateUserRoleRequest,
    ): ApiResponse<UpdateUserRoleResponse>
}
