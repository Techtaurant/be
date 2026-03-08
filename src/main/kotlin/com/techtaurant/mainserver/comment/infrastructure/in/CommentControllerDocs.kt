package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentResponse
import com.techtaurant.mainserver.comment.dto.CreateCommentRequest
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "댓글", description = "댓글 API")
interface CommentControllerDocs {
    @Operation(summary = "댓글 작성", description = "새 댓글 또는 대댓글을 작성합니다")
    @SwaggerApiResponse(
        responseCode = "201",
        description = "작성 성공",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiResponse::class))],
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(CommentStatus::class, ["COMMENT_PARENT_MISMATCH", "COMMENT_MAX_DEPTH_EXCEEDED"]),
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND"]),
            ApiErrorCodeResponse(CommentStatus::class, ["COMMENT_NOT_FOUND"]),
            ApiErrorCodeResponse(UserStatus::class, ["ID_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun createComment(
        userId: UUID,
        request: CreateCommentRequest,
    ): ApiResponse<CommentResponse>
}
