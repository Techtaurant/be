package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentContentListResponse
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

@Tag(name = "댓글", description = "댓글 API")
interface CommentReadOpenApiV2ControllerDocs {
    @Operation(
        summary = "부모 댓글 공개 콘텐츠 목록 조회",
        description =
                "SSG/ISR 캐싱에 적합한 부모 댓글 공개 콘텐츠 목록을 조회합니다. " +
                "좋아요수와 삭제 여부는 GET /open-api/comments/metadata?commentIds=... API로 분리되었습니다. " +
                "작성자 이름과 프로필 이미지는 GET /open-api/users/profile-images?userIds=... API를 사용하세요. " +
                "로그인 사용자의 좋아요/차단 상태는 GET /api/comments/me/states?commentIds=... API를 사용하세요.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getParentCommentContents(
        @Parameter(description = "게시물 ID") postId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentContentListResponse>>

    @Operation(
        summary = "대댓글 공개 콘텐츠 목록 조회",
        description =
                "SSG/ISR 캐싱에 적합한 대댓글 공개 콘텐츠 목록을 조회합니다. " +
                "좋아요수와 삭제 여부는 GET /open-api/comments/metadata?commentIds=... API로 분리되었습니다. " +
                "작성자 이름과 프로필 이미지는 GET /open-api/users/profile-images?userIds=... API를 사용하세요. " +
                "로그인 사용자의 좋아요/차단 상태는 GET /api/comments/me/states?commentIds=... API를 사용하세요.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getReplyContents(
        @Parameter(description = "부모 댓글 ID") commentId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentContentListResponse>>
}
