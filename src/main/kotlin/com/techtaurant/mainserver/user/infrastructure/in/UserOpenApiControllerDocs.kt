package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestAndUnknown
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.user.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "사용자", description = "사용자 Open API")
interface UserOpenApiControllerDocs {
    @Operation(summary = "사용자 검색", description = "사용자 이름으로 검색합니다")
    @SwaggerApiResponse(
        responseCode = "200",
        description = "검색 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun searchByName(
        @Parameter(description = "검색할 사용자 이름 (1자 이상)")
        @NotBlank
        name: String,
    ): ApiResponse<List<UserResponse>>

    @Operation(
        summary = "사용자 게시물 목록 조회",
        description = "특정 사용자의 게시물 목록을 커서 기반 페이지네이션으로 조회합니다. 본인 조회 시 DRAFT/PRIVATE 포함, 타인 조회 시 PUBLISHED만 반환됩니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getPostsByUserId(
        @Parameter(description = "조회 대상 사용자 ID") userId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "기간 필터 (WEEK: 7일, MONTH: 30일, YEAR: 365일, ALL: 전체)") period: PostPeriod,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, VIEW: 조회순, LIKE: 추천순, COMMENT: 댓글순)") sort: PostSortType,
        @Parameter(description = "카테고리 ID 필터 (생략 시 전체)") categoryId: UUID?,
        currentUserId: UUID?,
    ): ApiResponse<CursorPageResponse<PostListItemResponse>>
}
