package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.common.util.HttpRequestUtils
import com.techtaurant.mainserver.link.application.LinkViewLogService
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/links")
@Validated
class LinkViewLogOpenApiController(
    private val linkViewLogService: LinkViewLogService,
) : LinkViewLogOpenApiControllerDocs {
    @ApiErrorResponses
    @PostMapping("/{linkId}/view-logs")
    override fun recordLinkView(
        @PathVariable linkId: UUID,
        request: HttpServletRequest,
        @AuthenticationPrincipal userId: UUID?,
    ): ApiResponse<Unit> {
        linkViewLogService.recordView(
            linkId = linkId,
            userId = userId,
            ipAddress = HttpRequestUtils.extractIpAddress(request),
            userAgent = request.getHeader("User-Agent"),
        )

        return ApiResponse.ok(Unit)
    }
}
