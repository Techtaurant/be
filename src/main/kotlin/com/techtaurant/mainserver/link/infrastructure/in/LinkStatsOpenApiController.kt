package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.link.application.LinkStatsReadService
import com.techtaurant.mainserver.link.dto.LinkStatsResponse
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/links")
@Validated
class LinkStatsOpenApiController(
    private val linkStatsReadService: LinkStatsReadService,
) : LinkStatsOpenApiControllerDocs {
    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/stats")
    override fun getLinkStats(
        @RequestParam @Size(max = 100) linkIds: List<UUID>,
    ): ApiResponse<List<LinkStatsResponse>> {
        return ApiResponse.ok(linkStatsReadService.getLinkStats(linkIds))
    }
}
