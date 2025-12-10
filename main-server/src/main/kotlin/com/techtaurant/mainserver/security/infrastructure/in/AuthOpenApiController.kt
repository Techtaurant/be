package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.aop.AuthRestController
import com.techtaurant.mainserver.security.service.TokenRefreshService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@AuthRestController
@RequestMapping("/open-api/auth")
class AuthOpenApiController(
    private val tokenRefreshService: TokenRefreshService,
) {

    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "토큰 갱신 성공"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Refresh Token이 없거나 유효하지 않음"
            ),
        ]
    )
    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): ApiResponse<Unit> {
        tokenRefreshService.execute(request = request, response = response)
        return ApiResponse.ok(Unit)
    }
}
