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
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/posts")
@Validated
class PostController(
    private val postWriteService: PostWriteService,
    private val postListReadService: PostListReadService,
    private val postDetailReadService: PostDetailReadService,
) : PostControllerDocs {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createPost(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: CreatePostRequest,
    ): ApiResponse<PostResponse> {
        return ApiResponse.created(postWriteService.createPost(userId, request))
    }

    @PatchMapping("/{postId}")
    override fun updatePost(
        @PathVariable postId: UUID,
        @Valid @RequestBody request: UpdatePostRequest,
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<PostResponse> {
        return ApiResponse.ok(postWriteService.updatePost(postId, request, userId))
    }

    @GetMapping("/drafts")
    override fun getMyDrafts(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<CursorPageResponse<DraftListItemResponse>> {
        return ApiResponse.ok(postListReadService.getMyDrafts(userId, cursor, size))
    }

    @GetMapping("/drafts/{postId}")
    override fun getDraftDetail(
        @PathVariable postId: UUID,
        @AuthenticationPrincipal userId: UUID,
    ): ApiResponse<PostDetailResponse> {
        return ApiResponse.ok(postDetailReadService.getPostDetail(postId, userId, null, null))
    }
}
