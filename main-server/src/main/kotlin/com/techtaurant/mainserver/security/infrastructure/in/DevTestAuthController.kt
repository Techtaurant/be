package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.security.dto.DevTestLoginRequest
import com.techtaurant.mainserver.security.service.DevTestAuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발 환경 전용 테스트 인증 컨트롤러
 *
 * dev 프로파일에서만 활성화되며, 테스트 사용자로 JWT 토큰을 발급받을 수 있다.
 */
@RestController
@Profile("dev")
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/dev/auth")
@Tag(name = "Dev Auth", description = "개발 환경 전용 인증 API")
class DevTestAuthController(
    private val devTestAuthService: DevTestAuthService,
) {
    @Operation(
        summary = "개발용 테스트 로그인",
        description = "테스트 사용자로 로그인하여 JWT 토큰을 쿠키로 발급받습니다. 사용자가 없으면 자동 생성됩니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "로그인 성공, 쿠키에 accessToken/refreshToken 설정",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "비밀번호 불일치",
            ),
        ],
    )
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: DevTestLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<Nothing> {
        devTestAuthService.execute(request, response)
        return ApiResponse.ok()
    }
}
