package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.cookie.CookieHelper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
class AuthController(
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
        cookieHelper.deleteCookie(response, "accessToken")
        cookieHelper.deleteCookie(response, "refreshToken")
        return ApiResponse.ok(Unit)
    }
}
