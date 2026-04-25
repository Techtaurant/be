package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.user.application.UserWriteService
import com.techtaurant.mainserver.user.dto.UpdateUserRoleRequest
import com.techtaurant.mainserver.user.dto.UpdateUserRoleResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.ADMIN_API_PREFIX}/users")
class AdminUserRoleController(
    private val userWriteService: UserWriteService,
) : AdminUserRoleControllerDocs {
    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @PatchMapping("/{targetUserId}/role")
    override fun updateUserRole(
        @PathVariable targetUserId: UUID,
        @Valid @RequestBody request: UpdateUserRoleRequest,
    ): ApiResponse<UpdateUserRoleResponse> {
        return ApiResponse.ok(userWriteService.updateUserRole(targetUserId, request.role))
    }
}
