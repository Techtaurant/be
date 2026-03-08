package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.application.CommentWriteService
import com.techtaurant.mainserver.comment.dto.CommentResponse
import com.techtaurant.mainserver.comment.dto.CreateCommentRequest
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.user.enums.UserStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 댓글 API 컨트롤러
 */
@RestController
@RequestMapping("/api/comments")
class CommentController(
    private val commentWriteService: CommentWriteService,
    private val commentReadService: CommentReadService,
) : CommentControllerDocs {
    /**
     * 댓글을 작성합니다.
     * 댓글 또는 대댓글을 생성할 수 있습니다.
     */
    @ApiErrorResponses(
        comments = [CommentStatus.COMMENT_PARENT_MISMATCH, CommentStatus.COMMENT_MAX_DEPTH_EXCEEDED, CommentStatus.COMMENT_NOT_FOUND],
        posts = [PostStatus.POST_NOT_FOUND],
        users = [UserStatus.ID_NOT_FOUND],
        includeAuthenticationErrors = true,
        includeValidationError = true,
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createComment(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreateCommentRequest,
    ): ApiResponse<CommentResponse> {
        return ApiResponse.created(commentWriteService.createComment(userId, request))
    }
}
