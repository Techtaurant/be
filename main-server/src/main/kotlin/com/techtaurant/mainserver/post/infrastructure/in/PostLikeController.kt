package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.post.application.PostLikeLogService
import com.techtaurant.mainserver.post.dto.RecordPostLikeRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Post", description = "게시물 API")
@RestController
@RequestMapping("/api/posts")
@Validated
class PostLikeController(
    private val postLikeLogService: PostLikeLogService,
) {
    @PostMapping("/{postId}/like")
    @Operation(summary = "게시글 좋아요/싫어요", description = "게시글에 대한 좋아요 또는 싫어요를 기록합니다. 인증된 사용자만 호출 가능합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "좋아요/싫어요 기록 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시글 또는 사용자를 찾을 수 없음",
            ),
        ],
    )
    fun recordLike(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable postId: UUID,
        @Valid @RequestBody request: RecordPostLikeRequest,
    ): ApiResponse<Unit> {
        postLikeLogService.recordLike(
            postId = postId,
            userId = userId,
            isLiked = request.isLiked,
        )

        return ApiResponse.ok(Unit)
    }
}
