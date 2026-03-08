package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.RecordPostReadRequest
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "게시물", description = "게시물 API")
interface PostReadLogControllerDocs {
    @Operation(
        summary = "게시물 읽음 상태 변경",
        description = "게시물에 대한 읽음 상태를 변경합니다. isRead=true: 읽음 표시, isRead=false: 안읽음 표시. 인증된 사용자만 호출 가능합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "읽음 상태 변경 성공",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ApiResponse::class))],
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND"]),
            ApiErrorCodeResponse(UserStatus::class, ["USER_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun toggleReadStatus(
        userId: UUID,
        postId: UUID,
        request: RecordPostReadRequest,
    ): ApiResponse<Unit>
}
