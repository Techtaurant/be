package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorResponses
import com.techtaurant.mainserver.post.application.PostMetadataReadService
import com.techtaurant.mainserver.post.dto.PostMetadataResponse
import com.techtaurant.mainserver.security.SecurityConstants
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/posts")
@Validated
class PostMetadataOpenApiController(
    private val postMetadataReadService: PostMetadataReadService,
) : PostMetadataOpenApiControllerDocs {
    @ApiErrorResponses(includeValidationError = true)
    @GetMapping("/metadata")
    override fun getPostMetadata(
        @RequestParam @Size(max = 100) postIds: List<UUID>,
    ): ApiResponse<List<PostMetadataResponse>> {
        return ApiResponse.ok(postMetadataReadService.getPostMetadata(postIds))
    }
}
