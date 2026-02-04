package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.post.application.PostViewLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Post", description = "게시물 API")
@RestController
@RequestMapping("/open-api/posts")
@Validated
class PostViewController(
    private val postViewLogService: PostViewLogService,
) {

    @PostMapping("/{postId}/view")
    @Operation(summary = "게시글 조회 로그 생성", description = "게시글 조회 이벤트를 기록합니다. 비회원도 호출 가능합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 로그 생성 성공"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시글을 찾을 수 없음"
            ),
        ]
    )
    fun recordView(
        @PathVariable postId: UUID,
        request: HttpServletRequest,
        @AuthenticationPrincipal userId: UUID?,
    ): ApiResponse<Unit> {
        val ipAddress = extractIpAddress(request)
        val userAgent = request.getHeader("User-Agent")

        postViewLogService.recordView(
            postId = postId,
            userId = userId,
            ipAddress = ipAddress,
            userAgent = userAgent,
        )

        return ApiResponse.ok(Unit)
    }

    /**
     * 클라이언트의 IP 주소를 추출합니다.
     * 프록시를 거친 경우 X-Forwarded-For 헤더에서 원본 IP를 추출하고,
     * 그렇지 않으면 request.remoteAddr를 사용합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private fun extractIpAddress(request: HttpServletRequest): String? {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",").firstOrNull()?.trim()
        } else {
            request.remoteAddr
        }
    }
}
