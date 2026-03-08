package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.post.application.CategoryReadService
import com.techtaurant.mainserver.post.dto.CategoryResponse
import com.techtaurant.mainserver.security.SecurityConstants
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/users/{userId}/categories")
class CategoryReadController(
    private val categoryReadService: CategoryReadService,
) : CategoryReadControllerDocs {
    @GetMapping
    override fun searchCategories(
        @PathVariable userId: UUID,
        @RequestParam(required = false) path: String?,
    ): ApiResponse<List<CategoryResponse>> {
        return ApiResponse.ok(categoryReadService.searchByPath(userId, path))
    }
}
