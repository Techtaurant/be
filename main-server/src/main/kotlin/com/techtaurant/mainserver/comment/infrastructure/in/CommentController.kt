package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.application.CommentWriteService
import com.techtaurant.mainserver.comment.dto.CommentResponse
import com.techtaurant.mainserver.comment.dto.CreateCommentRequest
import com.techtaurant.mainserver.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 댓글 API 컨트롤러
 */
@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentWriteService: CommentWriteService,
    private val commentReadService: CommentReadService,
) {
    /**
     * 댓글을 작성합니다.
     * 댓글 또는 대댓글을 생성할 수 있습니다.
     */
    @Operation(summary = "댓글 작성", description = "새 댓글 또는 대댓글을 작성합니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "작성 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (부모 댓글이 다른 게시물, 대댓글의 답글 시도 등)",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시물 또는 부모 댓글을 찾을 수 없음",
            ),
        ],
    )
    @PostMapping
    fun createComment(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreateCommentRequest,
    ): ApiResponse<CommentResponse> {
        return ApiResponse.ok(commentWriteService.createComment(userId, request))
    }
}
