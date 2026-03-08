package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.TagResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "태그", description = "태그 API")
interface TagReadControllerDocs {
    @Operation(
        summary = "태그 목록 조회",
        description = "게시물 개수 기준 내림차순으로 정렬된 태그 목록을 조회합니다. 검색어(name)가 있으면 부분 일치 검색을 수행합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getTags(
        @Parameter(description = "검색할 태그 이름 (부분 일치)") name: String?,
        @Parameter(description = "다음 페이지 커서 (첫 페이지는 null)") cursor: String?,
        @Parameter(description = "페이지 크기 (기본값: 20)") size: Int,
    ): ApiResponse<CursorPageResponse<TagResponse>>
}
