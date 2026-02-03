package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.application.PostListReadService
import com.techtaurant.mainserver.post.application.PostWriteService
import com.techtaurant.mainserver.post.dto.CreatePostRequest
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.dto.PostResponse
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Post", description = "게시물 API")
@RestController
@RequestMapping("/api/posts")
@Validated
class PostController(
    private val postWriteService: PostWriteService,
    private val postListReadService: PostListReadService,
) {

    @PostMapping
    @Operation(summary = "게시물 작성", description = "새 게시물을 작성합니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "작성 성공"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (제목 길이 초과, 카테고리 depth 초과 등)"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자"
            ),
        ]
    )
    fun createPost(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreatePostRequest,
    ): ApiResponse<PostResponse> {
        return ApiResponse.ok(postWriteService.createPost(userId, request))
    }
}

@Tag(name = "Post", description = "게시물 API")
@RestController
@RequestMapping("/open-api/posts")
@Validated
class PostReadController(
    private val postListReadService: PostListReadService,
) {

    @Operation(
        summary = "게시물 목록 조회",
        description = "커서 기반 페이지네이션으로 게시물 목록을 조회합니다. 기간 필터와 정렬 조건을 적용할 수 있습니다."
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (size 범위 초과 등)"
            ),
        ]
    )
    @GetMapping
    fun getPosts(
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)")
        @RequestParam(required = false)
        cursor: String?,

        @Parameter(description = "페이지 크기 (1-100, 기본값 20)")
        @RequestParam(defaultValue = "20")
        @Min(1) @Max(100)
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
}
