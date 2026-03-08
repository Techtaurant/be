package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.post.application.PostReadLogService
import com.techtaurant.mainserver.post.dto.RecordPostReadRequest
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
class PostReadLogController(
    private val postReadLogService: PostReadLogService,
) : PostReadLogControllerDocs {
    @PostMapping("/{postId}/read-logs")
    override fun toggleReadStatus(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable postId: UUID,
        @Valid @RequestBody request: RecordPostReadRequest,
    ): ApiResponse<Unit> {
        postReadLogService.toggleReadStatus(
            postId = postId,
            userId = userId,
            isRead = request.isRead,
        )

        return ApiResponse.ok(Unit)
    }
}
