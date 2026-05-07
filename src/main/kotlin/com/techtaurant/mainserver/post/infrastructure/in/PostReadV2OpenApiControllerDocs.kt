package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestAndUnknown
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.PostDetailV2Response
import com.techtaurant.mainserver.post.dto.PostListItemV2Response
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

@Tag(name = "게시물 v2", description = "SSG/ISR용 게시물 public API")
interface PostReadV2OpenApiControllerDocs {
    @Operation(
        summary = "게시물 목록 조회 v2",
        description = "SSG/ISR에 사용할 수 있도록 로그인 사용자별 필드(isRead)를 제외한 공개 게시물 목록을 조회합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getPosts(
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "기간 필터 (WEEK: 7일, MONTH: 30일, YEAR: 365일, ALL: 전체)") period: PostPeriod,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, VIEW: 조회순, LIKE: 추천순, COMMENT: 댓글순)") sort: PostSortType,
        @Parameter(description = "작성자 ID 필터 (생략 시 전체 조회)") authorId: UUID?,
        @Parameter(description = "카테고리 ID 필터 (authorId 지정 시에만 적용, 생략 시 전체)") categoryId: UUID?,
        @Parameter(description = "태그 UUID 필터 (여러 개 전달 시 OR 조건으로 조회)") tagIds: List<UUID>?,
    ): ApiResponse<CursorPageResponse<PostListItemV2Response>>

    @Operation(
        summary = "게시물 상세 조회 v2",
        description = "SSG/ISR에 사용할 수 있도록 로그인 사용자별 필드(likeStatus, isRead)를 제외한 공개 게시물 상세를 조회합니다.",
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
    ): ApiResponse<PostDetailV2Response>
}
