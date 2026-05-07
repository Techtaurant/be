package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.post.application.PostDetailReadService
import com.techtaurant.mainserver.post.dto.PostUserDataResponse
import com.techtaurant.mainserver.post.enums.PostStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/posts")
@Validated
class PostUserDataController(
    private val postDetailReadService: PostDetailReadService,
) : PostUserDataControllerDocs {
    @ApiErrorResponses(posts = [PostStatus.POST_NOT_FOUND], includeAuthenticationErrors = true)
    @GetMapping("/{postId}/user-data")
    override fun getPostUserData(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable postId: UUID,
    ): ApiResponse<PostUserDataResponse> {
        return ApiResponse.ok(postDetailReadService.getPostUserData(postId, userId))
    }
}
