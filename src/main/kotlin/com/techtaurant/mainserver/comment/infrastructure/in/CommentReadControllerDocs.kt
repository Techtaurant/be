package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentListResponse
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "댓글", description = "댓글 API")
interface CommentReadControllerDocs {
    @Operation(
        summary = "부모 댓글 목록 조회",
        description = "커서 기반 페이지네이션으로 게시물의 부모 댓글(depth=0) 목록을 조회합니다. 정렬 조건을 적용할 수 있습니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiResponse::class))],
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getParentComments(
        userId: UUID?,
        @Parameter(description = "게시물 ID") postId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>>

    @Operation(
        summary = "대댓글 목록 조회",
        description = "커서 기반 페이지네이션으로 특정 부모 댓글의 대댓글(depth=1) 목록을 조회합니다. 정렬 조건을 적용할 수 있습니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiResponse::class))],
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST", "UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getReplies(
        userId: UUID?,
        @Parameter(description = "부모 댓글 ID") commentId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>>
}
