package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.post.application.PostViewerStateReadService
import com.techtaurant.mainserver.post.dto.PostViewerStateResponse
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.API_PREFIX}/posts")
@Validated
class PostViewerStateController(
    private val postViewerStateReadService: PostViewerStateReadService,
) : PostViewerStateControllerDocs {
    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @GetMapping("/me/states")
    override fun getPostViewerStates(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam @Size(max = 100) postIds: List<UUID>,
    ): ApiResponse<List<PostViewerStateResponse>> {
        return ApiResponse.ok(postViewerStateReadService.getPostViewerStates(userId, postIds))
    }
}
