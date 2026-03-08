package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.user.application.UserReadService
import com.techtaurant.mainserver.user.dto.UserResponse
import com.techtaurant.mainserver.user.enums.UserStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.API_PREFIX}/users")
class UserController(
    private val userReadService: UserReadService,
) : UserControllerDocs {
    @ApiErrorResponses(users = [UserStatus.ID_NOT_FOUND], includeAuthenticationErrors = true)
    @GetMapping("/me")
    override fun getMe(
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<UserResponse> {
        return ApiResponse.ok(userReadService.getMe(userId))
    }
}
