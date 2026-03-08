package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.security.aop.AuthRestController
import com.techtaurant.mainserver.security.service.LogoutService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@AuthRestController
@RequestMapping("${SecurityConstants.API_PREFIX}/auth")
class AuthApiController(
    private val logoutService: LogoutService,
) : AuthApiControllerDocs {
    @PostMapping("/logout")
    override fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<Unit> {
        logoutService.logout(request, response)
        return ApiResponse.ok(Unit)
    }
}
