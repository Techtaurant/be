package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.security.dto.DevTestLoginRequest
import com.techtaurant.mainserver.security.service.DevTestAuthService
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
class DevTestAuthController(
    private val devTestAuthService: DevTestAuthService,
) : DevTestAuthControllerDocs {
    @PostMapping("/login")
    override fun login(
        @RequestBody @Valid request: DevTestLoginRequest,
        response: HttpServletResponse,
    ): ApiResponse<Unit> {
        devTestAuthService.execute(request, response)
        return ApiResponse.ok()
    }
}
