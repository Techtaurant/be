package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentViewerStateResponse
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "댓글", description = "댓글 API")
interface CommentViewerStateControllerDocs {
    @Operation(
        summary = "댓글 로그인 사용자 상태 목록 조회",
        description =
            "commentIds에 해당하는 댓글에 대해 현재 로그인 사용자의 좋아요 상태와 " +
                "차단한 작성자의 댓글인지 여부를 batch로 조회합니다. 인증된 사용자만 호출할 수 있습니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["BAD_REQUEST"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getCommentViewerStates(
        userId: UUID,
        @Parameter(description = "조회할 댓글 ID 목록 (최대 100개)", required = true)
        @Size(max = 100)
        commentIds: List<UUID>,
    ): ApiResponse<List<CommentViewerStateResponse>>
}
