package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentListV2Response
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestAndUnknown
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "댓글 v2", description = "SSG/ISR용 댓글 public API")
interface CommentReadV2OpenApiControllerDocs {
    @Operation(
        summary = "부모 댓글 목록 조회 v2",
        description = "SSG/ISR에 사용할 수 있도록 로그인 사용자별 필드(likeStatus, isBanned)와 차단 마스킹을 제외한 부모 댓글 목록을 조회합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getParentComments(
        @Parameter(description = "게시물 ID") postId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListV2Response>>

    @Operation(
        summary = "대댓글 목록 조회 v2",
        description = "SSG/ISR에 사용할 수 있도록 로그인 사용자별 필드(likeStatus, isBanned)와 차단 마스킹을 제외한 대댓글 목록을 조회합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getReplies(
        @Parameter(description = "부모 댓글 ID") commentId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListV2Response>>
}
