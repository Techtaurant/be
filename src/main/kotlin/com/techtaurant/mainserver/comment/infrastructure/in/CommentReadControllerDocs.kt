package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentListResponse
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
interface CommentReadControllerDocs {
    @Operation(
        summary = "부모 댓글 목록 조회",
        description =
            "[Deprecated] 이 API는 공개 콘텐츠, 공개 동적 메타데이터, 로그인 사용자 상태가 하나의 응답에 섞여 있습니다. " +
                "공개 콘텐츠 목록은 GET /open-api/v2/posts/{postId}/comments, 공개 동적 메타데이터는 " +
                "GET /open-api/comments/metadata?commentIds=..., 작성자 프로필 이미지는 " +
                "GET /open-api/users/profile-images?userIds=..., 로그인 사용자 상태는 " +
                "GET /api/comments/me/states?commentIds=... API로 대체되었습니다.",
        deprecated = true,
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getParentComments(
        userId: UUID?,
        @Parameter(description = "게시물 ID") postId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>>

    @Operation(
        summary = "대댓글 목록 조회",
        description =
            "[Deprecated] 이 API는 공개 콘텐츠, 공개 동적 메타데이터, 로그인 사용자 상태가 하나의 응답에 섞여 있습니다. " +
                "공개 콘텐츠 목록은 GET /open-api/v2/comments/{commentId}/replies, 공개 동적 메타데이터는 " +
                "GET /open-api/comments/metadata?commentIds=..., 작성자 프로필 이미지는 " +
                "GET /open-api/users/profile-images?userIds=..., 로그인 사용자 상태는 " +
                "GET /api/comments/me/states?commentIds=... API로 대체되었습니다.",
        deprecated = true,
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getReplies(
        userId: UUID?,
        @Parameter(description = "부모 댓글 ID") commentId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)") cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)") @Min(1) @Max(100) size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>>
}
