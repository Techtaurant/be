package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentMetadataReadService
import com.techtaurant.mainserver.comment.dto.CommentMetadataResponse
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/comments")
@Validated
class CommentMetadataOpenApiController(
    private val commentMetadataReadService: CommentMetadataReadService,
) : CommentMetadataOpenApiControllerDocs {
    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/metadata")
    override fun getCommentMetadata(
        @RequestParam @Size(max = 100) commentIds: List<UUID>,
    ): ApiResponse<List<CommentMetadataResponse>> {
        return ApiResponse.ok(commentMetadataReadService.getCommentMetadata(commentIds))
    }
}
