package com.techtaurant.mainserver.attachment.infrastructure.`in`

import com.techtaurant.mainserver.attachment.dto.PresignedUrlRequest
import com.techtaurant.mainserver.attachment.dto.PresignedUrlResponse
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.swagger.ApiCommonBadRequestUnknownAndAuthenticationRequired
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Attachment", description = "첨부파일 API")
interface AttachmentControllerDocs {
    @Operation(
        summary = "Presigned URL 발급",
        description = "S3 직접 업로드를 위한 PUT Presigned URL을 발급합니다. 응답의 objectKey를 게시물 content에 삽입하세요.",
    )
    @SwaggerApiResponse(
        responseCode = "200",
        description = "Presigned URL 발급 성공",
    )
    @ApiCommonBadRequestUnknownAndAuthenticationRequired
    fun issuePresignedUploadUrl(
        userId: UUID,
        @Valid request: PresignedUrlRequest,
    ): ApiResponse<PresignedUrlResponse>
}
