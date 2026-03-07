package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.post.application.CategoryReadService
import com.techtaurant.mainserver.post.dto.CategoryResponse
import com.techtaurant.mainserver.security.SecurityConstants
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Category", description = "카테고리 API")
@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/users/{userId}/categories")
class CategoryReadController(
    private val categoryReadService: CategoryReadService,
) {
    @Operation(
        summary = "카테고리 path prefix 검색",
        description = "특정 유저의 카테고리를 path prefix로 검색합니다. 해당 경로로 시작하는 모든 카테고리를 반환합니다. path가 없으면 전체 카테고리를 반환합니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
        ],
    )
    @GetMapping
    fun searchCategories(
        @Parameter(description = "유저 ID")
        @PathVariable userId: UUID,
        @Parameter(description = "카테고리 path prefix (해당 경로로 시작하는 카테고리 검색)", example = "java/spring")
        @RequestParam(required = false) path: String?,
    ): ApiResponse<List<CategoryResponse>> {
        return ApiResponse.ok(categoryReadService.searchByPath(userId, path))
    }
}
