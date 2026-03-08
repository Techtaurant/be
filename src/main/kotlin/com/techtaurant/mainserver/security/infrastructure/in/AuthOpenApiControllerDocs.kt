package com.techtaurant.mainserver.security.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "인증", description = "인증 API")
interface AuthOpenApiControllerDocs {
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다")
    @SwaggerApiResponse(
        responseCode = "200",
        description = "토큰 갱신 성공",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiResponse::class))],
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(
                JwtStatus::class,
                [
                    "INVALID_REFRESH_TOKEN",
                    "MISSING_REFRESH_TOKEN",
                    "REFRESH_TOKEN_EXPIRED",
                    "INVALID_TOKEN",
                    "MALFORMED_TOKEN",
                    "UNSUPPORTED_TOKEN",
                ],
            ),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<Nothing>
}
