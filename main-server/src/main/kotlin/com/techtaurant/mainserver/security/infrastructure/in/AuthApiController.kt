package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.aop.AuthRestController
import com.techtaurant.mainserver.security.helper.CookieHelper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@AuthRestController
@RequestMapping("/api/auth")
class AuthApiController(
    private val cookieHelper: CookieHelper,

    ) {
    @Operation(summary = "로그아웃", description = "쿠키에서 토큰을 삭제하여 로그아웃합니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "로그아웃 성공"
            ),
        ]
    )
    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ApiResponse<Unit> {
        cookieHelper.deleteAllAuthCookies(response)
        return ApiResponse.ok(Unit)
    }
}
