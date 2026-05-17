package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.lock.LockStatus
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponse
import com.techtaurant.mainserver.common.swagger.ApiErrorCodeResponses
import com.techtaurant.mainserver.post.dto.CreatePostRequest
import com.techtaurant.mainserver.post.dto.DraftListItemResponse
import com.techtaurant.mainserver.post.dto.PostDetailResponse
import com.techtaurant.mainserver.post.dto.PostResponse
import com.techtaurant.mainserver.post.dto.UpdatePostRequest
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.security.jwt.JwtStatus
import com.techtaurant.mainserver.user.enums.UserStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "게시물", description = "게시물 API")
interface PostControllerDocs {
    @Operation(summary = "게시물 작성", description = "새 게시물을 작성합니다")
    @SwaggerApiResponse(
        responseCode = "201",
        description = "작성 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(PostStatus::class, ["TITLE_REQUIRED", "CONTENT_REQUIRED", "CATEGORY_DEPTH_EXCEEDED"]),
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(LockStatus::class, ["LOCK_ACQUISITION_FAILED"]),
            ApiErrorCodeResponse(UserStatus::class, ["ID_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun createPost(
        userId: UUID,
        @Valid request: CreatePostRequest,
    ): ApiResponse<PostResponse>

    @Operation(
        summary = "게시물 수정",
        description = "게시물의 내용을 수정하거나 상태를 전환합니다. 작성자만 수정 가능합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "수정 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(
                PostStatus::class,
                ["TITLE_REQUIRED", "CONTENT_REQUIRED", "CATEGORY_DEPTH_EXCEEDED", "CANNOT_MODIFY_OTHERS_POST", "POST_NOT_FOUND"],
            ),
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(LockStatus::class, ["LOCK_ACQUISITION_FAILED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun updatePost(
        postId: UUID,
        @Valid request: UpdatePostRequest,
        userId: UUID,
    ): ApiResponse<PostResponse>

    @Operation(
        summary = "내 임시 저장 게시물 목록 조회 (커서 기반)",
        description = "현재 사용자가 작성한 DRAFT 상태의 게시물 목록을 커서 기반 페이지네이션으로 조회합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getMyDrafts(
        cursor: String?,
        size: Int,
        userId: UUID,
    ): ApiResponse<CursorPageResponse<DraftListItemResponse>>

    @Operation(
        summary = "임시 저장 게시물 상세 조회",
        description = "DRAFT 상태의 게시물 상세 정보를 조회합니다. 작성자만 조회 가능합니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun getDraftDetail(
        postId: UUID,
        userId: UUID,
    ): ApiResponse<PostDetailResponse>

    @Operation(
        summary = "게시물 삭제",
        description = "게시물과 연관된 S3 첨부파일을 함께 삭제합니다. 작성자만 삭제 가능합니다.",
    )
    @SwaggerApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiErrorCodeResponses(
        [
            ApiErrorCodeResponse(PostStatus::class, ["POST_NOT_FOUND", "CANNOT_MODIFY_OTHERS_POST"]),
            ApiErrorCodeResponse(JwtStatus::class, ["AUTHENTICATION_REQUIRED"]),
            ApiErrorCodeResponse(DefaultStatus::class, ["UNKNOWN_EXCEPTION"]),
        ],
    )
    fun deletePost(
        postId: UUID,
        userId: UUID,
    )
}
