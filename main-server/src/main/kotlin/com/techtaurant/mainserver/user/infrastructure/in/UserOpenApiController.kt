package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.application.PostListReadService
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.security.SecurityConstants
import com.techtaurant.mainserver.user.application.UserReadService
import com.techtaurant.mainserver.user.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "User", description = "사용자 Open API")
@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/users")
@Validated
class UserOpenApiController(
    private val userReadService: UserReadService,
    private val postListReadService: PostListReadService,
) {
    companion object {
        private const val VALIDATION_ERROR_EXAMPLE =
            "{\"status\": 400," +
                " \"data\": {\"errors\":" +
                " {\"getPostsByUserId.size\":" +
                " \"100 이하여야 합니다\"}}," +
                " \"message\": \"Wrong Request\"}"
    }

    @Operation(summary = "사용자 검색", description = "사용자 이름으로 검색합니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "검색 성공",
            ),
        ],
    )
    @GetMapping("/search")
    fun searchByName(
        @RequestParam name: String,
    ): ApiResponse<List<UserResponse>> {
        return ApiResponse.ok(userReadService.searchByName(name))
    }

    @Operation(
        summary = "사용자 게시물 목록 조회",
        description = "특정 사용자의 게시물 목록을 커서 기반 페이지네이션으로 조회합니다. 본인 조회 시 DRAFT/PRIVATE 포함, 타인 조회 시 PUBLISHED만 반환됩니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (size 범위 초과 등)",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Validation 에러",
                                value = VALIDATION_ERROR_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/{userId}/posts")
    fun getPostsByUserId(
        @Parameter(description = "조회 대상 사용자 ID")
        @PathVariable
        userId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100)
        size: Int,
        @Parameter(description = "기간 필터 (WEEK: 7일, MONTH: 30일, YEAR: 365일, ALL: 전체)")
        @RequestParam(defaultValue = "ALL")
        period: PostPeriod,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, VIEW: 조회순, LIKE: 추천순, COMMENT: 댓글순)")
        @RequestParam(defaultValue = "LATEST")
        sort: PostSortType,
        @Parameter(description = "카테고리 ID 필터 (생략 시 전체)")
        @RequestParam(required = false)
        categoryId: UUID?,
        @AuthenticationPrincipal currentUserId: UUID?,
    ): ApiResponse<CursorPageResponse<PostListItemResponse>> {
        return ApiResponse.ok(
            postListReadService.getPostsByUserId(
                userId = userId,
                cursor = cursor,
                size = size,
                period = period,
                sortType = sort,
                categoryId = categoryId,
                currentUserId = currentUserId,
            ),
        )
    }
}
