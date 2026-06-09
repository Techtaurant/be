package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.link.application.LinkReadService
import com.techtaurant.mainserver.link.dto.LinkContentListItemResponse
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/companies")
@Validated
class CompanyLinkReadOpenApiController(
    private val linkReadService: LinkReadService,
) : CompanyLinkReadOpenApiControllerDocs {
    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/{companyUserId}/links")
    override fun getCompanyLinkContents(
        @PathVariable companyUserId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ApiResponse<CursorPageResponse<LinkContentListItemResponse>> {
        return ApiResponse.ok(
            linkReadService.getPublicCompanyLinkContents(
                companyUserId = companyUserId,
                cursor = cursor,
                size = size,
            ),
        )
    }
}
