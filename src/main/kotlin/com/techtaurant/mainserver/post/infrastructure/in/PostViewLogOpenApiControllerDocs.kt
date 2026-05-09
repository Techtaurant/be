package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.enums.PostStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "게시물", description = "게시물 API")
interface PostViewLogOpenApiControllerDocs {
    @Operation(
        summary = "게시물 조회 로그 기록",
        description =
            "게시물 상세 페이지 진입 등 실제 조회 이벤트가 발생했을 때 조회 로그를 기록하고 조회수를 증가시킵니다. " +
                "비로그인 사용자의 조회도 기록할 수 있으며, 로그인 사용자는 사용자 ID와 함께 기록됩니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 로그 기록 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun recordPostView(
        @Parameter(description = "게시물 ID") postId: UUID,
        @Parameter(hidden = true) request: HttpServletRequest,
        @Parameter(hidden = true) userId: UUID?,
    ): ApiResponse<Unit>
}
