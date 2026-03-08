package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.security.aop.AuthRestController
import com.techtaurant.mainserver.security.service.TokenRefreshService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@AuthRestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/auth")
class AuthOpenApiController(
    private val tokenRefreshService: TokenRefreshService,
) : AuthOpenApiControllerDocs {
    @PostMapping("/refresh")
    override fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<Nothing> {
        tokenRefreshService.execute(request = request, response = response)
        return ApiResponse.ok()
    }
}
