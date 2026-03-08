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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${SecurityConstants.OPEN_API_PREFIX}/users")
@Validated
class UserOpenApiController(
    private val userReadService: UserReadService,
    private val postListReadService: PostListReadService,
) : UserOpenApiControllerDocs {
    @GetMapping("/search")
    override fun searchByName(
        @NotBlank
        @RequestParam name: String,
    ): ApiResponse<List<UserResponse>> {
        return ApiResponse.ok(userReadService.searchByName(name))
    }

    @GetMapping("/{userId}/posts")
    override fun getPostsByUserId(
        @PathVariable userId: UUID,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
        @RequestParam(defaultValue = "ALL") period: PostPeriod,
        @RequestParam(defaultValue = "LATEST") sort: PostSortType,
        @RequestParam(required = false) categoryId: UUID?,
        @AuthenticationPrincipal currentUserId: UUID?,
    ): ApiResponse<CursorPageResponse<PostListItemResponse>> {
        return ApiResponse.ok(
            postListReadService.getPosts(
                cursor = cursor,
                size = size,
                period = period,
                sortType = sort,
                authorId = userId,
                categoryId = categoryId,
                currentUserId = currentUserId,
            ),
        )
    }
}
