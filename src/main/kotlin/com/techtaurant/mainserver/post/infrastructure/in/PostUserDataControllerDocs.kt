package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.PostUserDataResponse
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "게시물 사용자 데이터", description = "게시물 인증 사용자별 데이터 API")
interface PostUserDataControllerDocs {
    @Operation(
        summary = "게시물 사용자 데이터 조회",
        description = "로그인 사용자 기준 게시물 likeStatus와 isRead를 조회합니다. SSG/ISR용 게시물 public API와 분리해서 호출합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getPostUserData(
        userId: UUID,
        @Parameter(description = "게시물 ID") postId: UUID,
    ): ApiResponse<PostUserDataResponse>
}
