package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.dto.CommentListResponse
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 댓글 조회 API 컨트롤러 (비회원 접근 가능)
 */
@RestController
@RequestMapping("/open-api/comments")
@Validated
class CommentReadController(
    private val commentReadService: CommentReadService,
) : CommentReadControllerDocs {
    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/posts/{postId}")
    override fun getParentComments(
        @AuthenticationPrincipal userId: UUID?,
        @PathVariable postId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "LATEST") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>> {
        return ApiResponse.ok(commentReadService.getParentComments(postId, cursor, size, sort, userId))
    }

    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/{commentId}/replies")
    override fun getReplies(
        @AuthenticationPrincipal userId: UUID?,
        @PathVariable commentId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "LATEST") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>> {
        return ApiResponse.ok(commentReadService.getReplies(commentId, cursor, size, sort, userId))
    }
}
