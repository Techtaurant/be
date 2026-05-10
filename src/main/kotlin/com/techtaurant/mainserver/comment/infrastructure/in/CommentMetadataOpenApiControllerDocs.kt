package com.techtaurant.mainserver.comment.infrastructure.`in`

import com.techtaurant.mainserver.comment.dto.CommentMetadataResponse
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestAndUnknown
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Size
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "댓글", description = "댓글 API")
interface CommentMetadataOpenApiControllerDocs {
    @Operation(
        summary = "댓글 공개 메타데이터 목록 조회",
        description =
            "commentIds에 해당하는 댓글의 공개 동적 메타데이터를 batch로 조회합니다. " +
            "좋아요수, 대댓글수, 삭제 여부를 반환합니다. 존재하지 않는 댓글은 응답에서 제외됩니다.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "조회 성공",
    )
    @ApiCommonBadRequestAndUnknown
    fun getCommentMetadatas(
        @Parameter(description = "조회할 댓글 ID 목록 (최대 100개)", required = true)
        @Size(max = 100)
        commentIds: List<UUID>,
    ): ApiResponse<List<CommentMetadataResponse>>
}
