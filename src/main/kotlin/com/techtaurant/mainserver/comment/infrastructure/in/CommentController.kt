package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.application.CommentWriteService
import com.techtaurant.mainserver.comment.dto.CommentResponse
import com.techtaurant.mainserver.comment.dto.CreateCommentRequest
import com.techtaurant.mainserver.common.dto.ApiResponse
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
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createComment(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreateCommentRequest,
    ): ApiResponse<CommentResponse> {
        return ApiResponse.created(commentWriteService.createComment(userId, request))
    }
}
