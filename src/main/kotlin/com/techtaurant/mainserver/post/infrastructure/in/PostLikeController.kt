package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.post.application.PostLikeLogService
import com.techtaurant.mainserver.post.dto.RecordPostLikeRequest
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.user.enums.UserStatus
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
@RequestMapping("/api/posts")
@Validated
class PostLikeController(
    private val postLikeLogService: PostLikeLogService,
) : PostLikeControllerDocs {
    @ApiErrorResponses(
        posts = [PostStatus.POST_NOT_FOUND],
        users = [UserStatus.ID_NOT_FOUND],
        includeAuthenticationErrors = true,
        includeValidationError = true,
    )
    @PostMapping("/{postId}/like")
    override fun recordLike(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable postId: UUID,
        @Valid @RequestBody request: RecordPostLikeRequest,
    ): ApiResponse<Unit> {
        postLikeLogService.recordLike(
            postId = postId,
            userId = userId,
            likeStatus = request.likeStatus,
        )

        return ApiResponse.ok(Unit)
    }
}
