package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestAndUnknown
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.link.dto.LinkContentDetailResponse
import com.techtaurant.mainserver.link.dto.LinkContentListItemResponse
import com.techtaurant.mainserver.link.enums.LinkStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "링크", description = "링크 API")
interface LinkReadOpenApiControllerDocs {
    @Operation(
        summary = "링크 정적 콘텐츠 목록 조회",
        description =
            "[Deprecated] 이 API는 링크 생성일 단일 정렬만 지원하며 커서에 정렬 기준이 암묵적으로 인코딩되어 있습니다. " +
                "명시적 정렬(PUBLISHED/LIKE/SAVE)과 기간 필터를 지원하는 GET /open-api/v1/links API로 대체되었습니다. " +
                "기존 호환을 위해 생성일순 커서 페이지네이션 동작은 그대로 유지됩니다.",
        deprecated = true,
    )
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @ApiCommonBadRequestAndUnknown
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(LinkStatus::class, ["INVALID_LINK_CURSOR"]),
        ],
    )
    fun getLinkContents(
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "출처 회사 사용자 ID 필터 (생략 시 전체 조회)") sourceCompanyUserId: UUID?,
        @Parameter(description = "링크 태그명 필터 (생략 시 전체 조회)") tag: String?,
    ): ApiResponse<CursorPageResponse<LinkContentListItemResponse>>

    @Operation(
        summary = "링크 정적 콘텐츠 상세 조회",
        description =
            "SSG/ISR 캐싱에 적합한 링크 정적 콘텐츠 상세 정보를 조회합니다. " +
                "로그인 사용자의 저장/읽음 상태는 포함하지 않습니다.",
    )
    @SwaggerApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(LinkStatus::class, ["LINK_NOT_FOUND"]),
        ],
    )
    fun getLinkContentDetail(
        @Parameter(description = "링크 ID") linkId: UUID,
    ): ApiResponse<LinkContentDetailResponse>
}
