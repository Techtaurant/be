package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentLikeLogService
import com.techtaurant.mainserver.comment.dto.RecordCommentLikeRequest
import com.techtaurant.mainserver.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api/comments")
@Validated
class CommentLikeController(
    private val commentLikeLogService: CommentLikeLogService,
) {
    @PostMapping("/{commentId}/like")
    @Operation(summary = "댓글 좋아요/취소", description = "댓글에 대한 좋아요 또는 취소를 기록합니다. 인증된 사용자만 호출 가능합니다.")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "좋아요/취소 기록 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "댓글 또는 사용자를 찾을 수 없음",
            ),
        ],
    )
    fun recordLike(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable commentId: UUID,
        @Valid @RequestBody request: RecordCommentLikeRequest,
    ): ApiResponse<Unit> {
        commentLikeLogService.recordLike(
            commentId = commentId,
            userId = userId,
            isLiked = request.isLiked,
        )

        return ApiResponse.ok(Unit)
    }
}
