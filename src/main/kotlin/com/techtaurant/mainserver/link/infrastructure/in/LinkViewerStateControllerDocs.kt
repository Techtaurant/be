package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.link.dto.LinkViewerStateResponse
import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "링크", description = "회사 링크 조회 및 사용자 상호작용 API")
interface LinkViewerStateControllerDocs {
    @Operation(
        summary = "링크 로그인 사용자 상태 목록 조회",
        description =
            "linkIds에 해당하는 링크에 대해 현재 로그인 사용자의 저장 여부와 읽음 여부를 batch로 조회합니다. " +
                "인증된 사용자만 호출할 수 있습니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getLinkViewerStates(
        userId: UUID,
        @Parameter(description = "조회할 링크 ID 목록 (최대 100개)", required = true)
        @Size(max = 100)
        linkIds: List<UUID>,
    ): ApiResponse<List<LinkViewerStateResponse>>
}
