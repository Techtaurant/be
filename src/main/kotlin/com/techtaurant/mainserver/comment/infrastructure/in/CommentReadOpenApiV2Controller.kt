package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.dto.CommentContentListResponse
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/v2")
@Validated
class CommentReadOpenApiV2Controller(
    private val commentReadService: CommentReadService,
) : CommentReadOpenApiV2ControllerDocs {
    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/posts/{postId}/comments")
    override fun getParentCommentContents(
        @PathVariable postId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "LATEST") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentContentListResponse>> {
        return ApiResponse.ok(commentReadService.getParentCommentContents(postId, cursor, size, sort))
    }

    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/comments/{commentId}/replies")
    override fun getReplyContents(
        @PathVariable commentId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "LATEST") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentContentListResponse>> {
        return ApiResponse.ok(commentReadService.getReplyContents(commentId, cursor, size, sort))
    }
}
