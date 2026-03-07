package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.application.PostDetailReadService
import com.techtaurant.mainserver.post.application.PostListReadService
import com.techtaurant.mainserver.post.application.PostWriteService
import com.techtaurant.mainserver.post.dto.CreatePostRequest
import com.techtaurant.mainserver.post.dto.DraftListItemResponse
import com.techtaurant.mainserver.post.dto.PostDetailResponse
import com.techtaurant.mainserver.post.dto.PostResponse
import com.techtaurant.mainserver.post.dto.UpdatePostRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val postDetailReadService: PostDetailReadService,
) {
    @PostMapping
    @Operation(summary = "게시물 작성", description = "새 게시물을 작성합니다")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "작성 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (제목 길이 초과, 카테고리 depth 초과 등)",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자",
            ),
        ],
    )
    fun createPost(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreatePostRequest,
    ): ApiResponse<PostResponse> {
        return ApiResponse.ok(postWriteService.createPost(userId, request))
    }

    @PatchMapping("/{postId}")
    @Operation(
        summary = "게시물 수정",
        description = "게시물의 내용을 수정하거나 상태를 전환합니다. 작성자만 수정 가능합니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음 (다른 사용자의 게시물)",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시물을 찾을 수 없음",
            ),
        ],
    )
    fun updatePost(
        @PathVariable postId: UUID,
        @Valid @RequestBody request: UpdatePostRequest,
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<PostResponse> {
        return ApiResponse.ok(postWriteService.updatePost(postId, request, userId))
    }

    @GetMapping("/drafts")
    @Operation(
        summary = "내 임시 저장 게시물 목록 조회 (커서 기반)",
        description = "현재 사용자가 작성한 DRAFT 상태의 게시물 목록을 커서 기반 페이지네이션으로 조회합니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자",
            ),
        ],
    )
    fun getMyDrafts(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<CursorPageResponse<DraftListItemResponse>> {
        return ApiResponse.ok(postListReadService.getMyDrafts(userId, cursor, size))
    }

    @GetMapping("/drafts/{postId}")
    @Operation(
        summary = "임시 저장 게시물 상세 조회",
        description = "DRAFT 상태의 게시물 상세 정보를 조회합니다. 작성자만 조회 가능합니다.",
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시물을 찾을 수 없거나 권한 없음",
            ),
        ],
    )
    fun getDraftDetail(
        @PathVariable postId: UUID,
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<PostDetailResponse> {
        return ApiResponse.ok(postDetailReadService.getPostDetail(postId, userId, null, null))
    }
}
