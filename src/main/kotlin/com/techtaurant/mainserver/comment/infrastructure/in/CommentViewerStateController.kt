package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentViewerStateReadService
import com.techtaurant.mainserver.comment.dto.CommentViewerStateResponse
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
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
@RequestMapping("${SecurityConstants.API_PREFIX}/comments")
@Validated
class CommentViewerStateController(
    private val commentViewerStateReadService: CommentViewerStateReadService,
) : CommentViewerStateControllerDocs {
    @ApiErrorResponses(includeAuthenticationErrors = true, includeValidationError = true)
    @GetMapping("/me/states")
    override fun getCommentViewerStates(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam @Size(max = 100) commentIds: List<UUID>,
    ): ApiResponse<List<CommentViewerStateResponse>> {
        return ApiResponse.ok(commentViewerStateReadService.getCommentViewerStates(userId, commentIds))
    }
}
