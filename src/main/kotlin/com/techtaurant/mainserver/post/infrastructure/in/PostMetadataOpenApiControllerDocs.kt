package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestAndUnknown
import com.techtaurant.mainserver.post.dto.PostMetadataResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "게시물", description = "게시물 API")
interface PostMetadataOpenApiControllerDocs {
    @Operation(
        summary = "게시물 공개 메타데이터 목록 조회",
        description =
            "postIds에 해당하는 PUBLISHED 게시물의 공개 동적 메타데이터를 batch로 조회합니다. " +
                "조회수, 좋아요수, 댓글수, 상태, 썸네일/본문 첨부 presigned URL을 반환합니다. " +
                "작성자 이름과 프로필 이미지는 GET /open-api/users/profile-images?userIds=... API를 사용하세요. " +
                "존재하지 않거나 공개되지 않은 게시물은 응답에서 제외됩니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getPostMetadata(
        @Parameter(description = "조회할 게시물 ID 목록 (최대 100개)", required = true)
        @Size(max = 100)
        postIds: List<UUID>,
    ): ApiResponse<List<PostMetadataResponse>>
}
