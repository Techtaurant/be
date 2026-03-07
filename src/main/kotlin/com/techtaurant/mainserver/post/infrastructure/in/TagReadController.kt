package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.application.TagReadService
import com.techtaurant.mainserver.post.dto.TagResponse
import com.techtaurant.mainserver.security.SecurityConstants
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Tag", description = "태그 API")
@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/tags")
class TagReadController(
    private val tagReadService: TagReadService,
) {
    @Operation(
        summary = "태그 목록 조회",
        description = "게시물 개수 기준 내림차순으로 정렬된 태그 목록을 조회합니다. 검색어(name)가 있으면 부분 일치 검색을 수행합니다.",
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
    fun getTags(
        @Parameter(description = "검색할 태그 이름 (부분 일치)")
        @RequestParam(required = false) name: String?,
        @Parameter(description = "다음 페이지 커서 (첫 페이지는 null)")
        @RequestParam(required = false) cursor: String?,
        @Parameter(description = "페이지 크기 (기본값: 20)")
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CursorPageResponse<TagResponse>> {
        return ApiResponse.ok(tagReadService.getTagsWithPostCount(name, cursor, size))
    }
}
