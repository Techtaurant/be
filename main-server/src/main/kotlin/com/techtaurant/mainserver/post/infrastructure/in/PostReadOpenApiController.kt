package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.util.HttpRequestUtils
import com.techtaurant.mainserver.post.application.PostDetailReadService
import com.techtaurant.mainserver.post.application.PostListReadService
import com.techtaurant.mainserver.post.dto.PostDetailResponse
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
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
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Post", description = "게시물 API")
@RestController
@RequestMapping("/open-api/posts")
@Validated
class PostReadOpenApiController(
    private val postListReadService: PostListReadService,
    private val postDetailReadService: PostDetailReadService,
) {
    companion object {
        private const val VALIDATION_ERROR_EXAMPLE =
            "{\"status\": 400," +
                " \"data\": {\"errors\":" +
                " {\"getPosts.size\":" +
                " \"100 이하여야 합니다\"}}," +
                " \"message\": \"Wrong Request\"}"

        private const val POST_NOT_FOUND_EXAMPLE =
            "{\"status\": 3001," +
                " \"data\": null," +
                " \"message\": \"게시물을 찾을 수 없습니다\"}"
    }

    @Operation(
        summary = "게시물 목록 조회",
        description = "커서 기반 페이지네이션으로 게시물 목록을 조회합니다. 기간 필터와 정렬 조건을 적용할 수 있습니다. 로그인 시 본인의 DRAFT/PRIVATE 게시물도 포함되며, 비회원도 조회 가능합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 성공 (작성자 프로필 이미지, 게시물 썸네일, 읽음 여부 포함)",
            ),
            SwaggerApiResponse(
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
    @GetMapping
    fun getPosts(
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
    ): ApiResponse<CursorPageResponse<PostListItemResponse>> {
        return ApiResponse.ok(postListReadService.getPosts(cursor, size, period, sort))
    }

    @Operation(
        summary = "게시물 상세 조회",
        description = "게시물 상세 정보를 조회합니다. 조회 시 자동으로 조회 로그가 기록됩니다. 비회원도 조회 가능합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "게시물을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                        examples = [
                            ExampleObject(
                                name = "게시물 미존재",
                                value = POST_NOT_FOUND_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/{postId}")
    fun getPostDetail(
        @Parameter(description = "게시물 ID")
        @PathVariable postId: UUID,
        request: HttpServletRequest,
        @AuthenticationPrincipal userId: UUID?,
    ): ApiResponse<PostDetailResponse> {
        val ipAddress = HttpRequestUtils.extractIpAddress(request)
        val userAgent = request.getHeader("User-Agent")

        return ApiResponse.ok(
            postDetailReadService.getPostDetail(
                postId = postId,
                userId = userId,
                ipAddress = ipAddress,
                userAgent = userAgent,
            ),
        )
    }
}
