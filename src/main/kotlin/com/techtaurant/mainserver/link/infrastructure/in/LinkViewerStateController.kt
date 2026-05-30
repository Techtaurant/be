package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.link.application.LinkViewerStateReadService
import com.techtaurant.mainserver.link.dto.LinkViewerStateResponse
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.API_PREFIX}/links")
@Validated
class LinkViewerStateController(
    private val linkViewerStateReadService: LinkViewerStateReadService,
) : LinkViewerStateControllerDocs {
    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @GetMapping("/me/states")
    override fun getLinkViewerStates(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam @Size(max = 100) linkIds: List<UUID>,
    ): ApiResponse<List<LinkViewerStateResponse>> {
        return ApiResponse.ok(linkViewerStateReadService.getLinkViewerStates(userId, linkIds))
    }
}
