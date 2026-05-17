package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.link.enums.LinkStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "링크", description = "회사 링크 조회 및 사용자 상호작용 API")
interface LinkViewLogOpenApiControllerDocs {
    @Operation(
        summary = "링크 조회 로그 기록",
        description =
            "링크 클릭 등 실제 조회 이벤트가 발생했을 때 조회 로그를 기록하고 조회수를 증가시킵니다. " +
                "비로그인 사용자의 조회도 기록할 수 있으며, 로그인 사용자는 사용자 ID와 함께 기록됩니다.",
    )
    @SwaggerApiResponse(responseCode = "200", description = "조회 로그 기록 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(LinkStatus::class, ["LINK_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun recordLinkView(
        @Parameter(description = "링크 ID") linkId: UUID,
        @Parameter(hidden = true) request: HttpServletRequest,
        @Parameter(hidden = true) userId: UUID?,
    ): ApiResponse<Unit>
}
