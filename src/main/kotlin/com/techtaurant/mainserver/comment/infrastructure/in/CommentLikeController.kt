package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentLikeLogService
import com.techtaurant.mainserver.comment.dto.RecordCommentLikeRequest
import com.techtaurant.mainserver.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/comments")
@Validated
class CommentLikeController(
    private val commentLikeLogService: CommentLikeLogService,
) : CommentLikeControllerDocs {
    @PostMapping("/{commentId}/like")
    override fun recordLike(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable commentId: UUID,
        @Valid @RequestBody request: RecordCommentLikeRequest,
    ): ApiResponse<Unit> {
        commentLikeLogService.recordLike(
            commentId = commentId,
            userId = userId,
            likeStatus = request.likeStatus,
        )

        return ApiResponse.ok(Unit)
    }
}
