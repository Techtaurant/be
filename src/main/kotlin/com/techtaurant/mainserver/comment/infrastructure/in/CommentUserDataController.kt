package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.dto.CommentUserDataResponse
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/comments")
@Validated
class CommentUserDataController(
    private val commentReadService: CommentReadService,
) : CommentUserDataControllerDocs {
    @ApiErrorResponses(comments = [CommentStatus.COMMENT_NOT_FOUND], includeAuthenticationErrors = true)
    @GetMapping("/{commentId}/user-data")
    override fun getCommentUserData(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable commentId: UUID,
    ): ApiResponse<CommentUserDataResponse> {
        return ApiResponse.ok(commentReadService.getCommentUserData(commentId, userId))
    }
}
