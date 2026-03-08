package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.PostDetailResponse
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.enums.PostStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "게시물", description = "게시물 API")
interface PostReadOpenApiControllerDocs {
    @Operation(
        summary = "게시물 목록 조회",
        description = "커서 기반 페이지네이션으로 게시물 목록을 조회합니다. 기간 필터와 정렬 조건을 적용할 수 있습니다. 로그인 시 본인의 DRAFT/PRIVATE 게시물도 포함되며, 비회원도 조회 가능합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공 (작성자 프로필 이미지, 게시물 썸네일, 읽음 여부 포함)",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getPosts(
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "기간 필터 (WEEK: 7일, MONTH: 30일, YEAR: 365일, ALL: 전체)") period: PostPeriod,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, VIEW: 조회순, LIKE: 추천순, COMMENT: 댓글순)") sort: PostSortType,
        @Parameter(description = "작성자 ID 필터 (생략 시 전체 조회, 본인 조회 시 DRAFT/PRIVATE 포함)") authorId: UUID?,
        @Parameter(description = "카테고리 ID 필터 (authorId 지정 시에만 적용, 생략 시 전체)") categoryId: UUID?,
        currentUserId: UUID?,
    ): ApiResponse<CursorPageResponse<PostListItemResponse>>

    @Operation(
        summary = "게시물 상세 조회",
        description = "게시물 상세 정보를 조회합니다. 조회 시 자동으로 조회 로그가 기록됩니다. 비회원도 조회 가능합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getPostDetail(
        @Parameter(description = "게시물 ID") postId: UUID,
        request: HttpServletRequest,
        userId: UUID?,
    ): ApiResponse<PostDetailResponse>
}
