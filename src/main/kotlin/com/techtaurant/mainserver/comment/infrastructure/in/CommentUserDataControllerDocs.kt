package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentUserDataResponse
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "댓글 사용자 데이터", description = "댓글 인증 사용자별 데이터 API")
interface CommentUserDataControllerDocs {
    @Operation(
        summary = "댓글 사용자 데이터 조회",
        description = "로그인 사용자 기준 댓글 likeStatus와 isBannedAuthor를 조회합니다. SSG/ISR용 댓글 public API와 분리해서 호출합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(CommentStatus::class, ["COMMENT_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getCommentUserData(
        userId: UUID,
        @Parameter(description = "댓글 ID") commentId: UUID,
    ): ApiResponse<CommentUserDataResponse>
}
