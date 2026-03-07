package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.application.CommentReadService
import com.techtaurant.mainserver.comment.dto.CommentListResponse
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

/**
 * 댓글 조회 API 컨트롤러 (비회원 접근 가능)
 */
@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/open-api/comments")
@Validated
class CommentReadController(
    private val commentReadService: CommentReadService,
) {
    @Operation(
        summary = "부모 댓글 목록 조회",
        description = "커서 기반 페이지네이션으로 게시물의 부모 댓글(depth=0) 목록을 조회합니다. 정렬 조건을 적용할 수 있습니다.",
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
            ),
        ],
    )
    @GetMapping("/posts/{postId}")
    fun getParentComments(
        @AuthenticationPrincipal userId: UUID?,
        @Parameter(description = "게시물 ID")
        @PathVariable postId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100)
        size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)")
        @RequestParam(defaultValue = "LATEST")
        sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>> {
        return ApiResponse.ok(commentReadService.getParentComments(postId, cursor, size, sort, userId))
    }

    @Operation(
        summary = "대댓글 목록 조회",
        description = "커서 기반 페이지네이션으로 특정 부모 댓글의 대댓글(depth=1) 목록을 조회합니다. 정렬 조건을 적용할 수 있습니다.",
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
            ),
        ],
    )
    @GetMapping("/{commentId}/replies")
    fun getReplies(
        @AuthenticationPrincipal userId: UUID?,
        @Parameter(description = "부모 댓글 ID")
        @PathVariable commentId: UUID,
        @Parameter(description = "이전 응답의 nextCursor (첫 페이지는 생략)")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "페이지 크기 (1-100, 기본값 20)")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(100)
        size: Int,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, LIKE: 추천순, REPLY: 답글순)")
        @RequestParam(defaultValue = "LATEST")
        sort: CommentSortType,
    ): ApiResponse<CursorPageResponse<CommentListResponse>> {
        return ApiResponse.ok(commentReadService.getReplies(commentId, cursor, size, sort, userId))
    }
}
