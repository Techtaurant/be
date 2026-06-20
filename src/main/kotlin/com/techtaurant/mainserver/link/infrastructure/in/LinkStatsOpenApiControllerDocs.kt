package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestAndUnknown
import com.techtaurant.mainserver.link.dto.LinkStatsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "링크", description = "링크 API")
interface LinkStatsOpenApiControllerDocs {
    @Operation(
        summary = "링크 통계 합계 목록 조회",
        description =
            "linkIds에 해당하는 링크의 누적 통계(조회수/좋아요수/저장수 합계)를 batch로 조회합니다. " +
                "각 카운트는 일별 통계(link_daily_stats)를 전 기간 합산한 값입니다. " +
                "통계 이력이 없는(=한 번도 조회/좋아요/저장되지 않은) 링크는 응답에서 제외됩니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getLinkStats(
        @Parameter(description = "조회할 링크 ID 목록 (최대 100개)", required = true)
        @Size(max = 100)
        linkIds: List<UUID>,
    ): ApiResponse<List<LinkStatsResponse>>
}
