package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentDeleteService
import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.application.CommentWriteService
import com.techtaurant.mainserver.comment.dto.CommentResponse
import com.techtaurant.mainserver.comment.dto.CreateCommentRequest
import com.techtaurant.mainserver.comment.dto.UpdateCommentRequest
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.user.enums.UserStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val commentDeleteService: CommentDeleteService,
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

    /**
     * 댓글 내용을 수정합니다.
     * 본인 댓글만 수정 가능합니다.
     */
    @ApiErrorResponses(
        comments = [CommentStatus.COMMENT_NOT_FOUND, CommentStatus.COMMENT_ALREADY_DELETED, CommentStatus.COMMENT_AUTHOR_MISMATCH],
        includeAuthenticationErrors = true,
        includeValidationError = true,
    )
    @PatchMapping("/{commentId}")
    override fun updateComment(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable commentId: UUID,
        @Valid @RequestBody request: UpdateCommentRequest,
    ): ApiResponse<CommentResponse> {
        return ApiResponse.ok(commentWriteService.updateComment(commentId, userId, request))
    }

    /**
     * 댓글을 삭제합니다.
     * 본인 댓글만 삭제 가능하며, 내용은 블라인드 처리됩니다.
     */
    @ApiErrorResponses(
        comments = [CommentStatus.COMMENT_NOT_FOUND, CommentStatus.COMMENT_ALREADY_DELETED, CommentStatus.COMMENT_AUTHOR_MISMATCH],
        includeAuthenticationErrors = true,
    )
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteComment(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable commentId: UUID,
    ) {
        commentDeleteService.deleteComment(commentId, userId)
    }
}
