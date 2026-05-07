package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.dto.CommentListV2Response
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
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
@RequestMapping("/open-api/v2/comments")
@Validated
class CommentReadV2OpenApiController(
    private val commentReadService: CommentReadService,
) : CommentReadV2OpenApiControllerDocs {
    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/posts/{postId}")
    override fun getParentComments(
        @PathVariable postId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "LATEST") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListV2Response>> {
        return ApiResponse.ok(commentReadService.getPublicParentCommentsV2(postId, cursor, size, sort))
    }

    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/{commentId}/replies")
    override fun getReplies(
        @PathVariable commentId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "LATEST") sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListV2Response>> {
        return ApiResponse.ok(commentReadService.getPublicRepliesV2(commentId, cursor, size, sort))
    }
}
