package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.application.TagReadService
import com.techtaurant.mainserver.post.dto.TagResponse
import com.techtaurant.mainserver.security.SecurityConstants
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/tags")
class TagReadController(
    private val tagReadService: TagReadService,
) : TagReadControllerDocs {
    @GetMapping
    override fun getTags(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CursorPageResponse<TagResponse>> {
        return ApiResponse.ok(tagReadService.getTagsWithPostCount(name, cursor, size))
    }
}
