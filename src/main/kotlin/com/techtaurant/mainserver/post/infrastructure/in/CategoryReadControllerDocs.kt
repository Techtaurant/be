package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.CategoryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "카테고리", description = "카테고리 API")
interface CategoryReadControllerDocs {
    @Operation(
        summary = "카테고리 path prefix 검색",
        description = "특정 유저의 카테고리를 path prefix로 검색합니다. 해당 경로로 시작하는 모든 카테고리를 반환합니다. path가 없으면 전체 카테고리를 반환합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiResponse::class))],
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun searchCategories(
        @Parameter(description = "유저 ID") userId: UUID,
        @Parameter(description = "카테고리 path prefix (해당 경로로 시작하는 카테고리 검색)", example = "java/spring") path: String?,
    ): ApiResponse<List<CategoryResponse>>
}
