package com.techtaurant.mainserver.attachment.dto

import com.techtaurant.mainserver.attachment.entity.Attachment
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Presigned URL 발급 응답")
data class PresignedUrlResponse(
    @field:Schema(description = "S3 PUT Presigned URL", example = "https://techtaurant-media.s3.ap-northeast-2.amazonaws.com/tmp/...")
    val presignedUrl: String,
    @field:Schema(description = "S3 오브젝트 키 (content에 삽입할 값)", example = "tmp/550e8400-e29b-41d4-a716-446655440000/photo.jpg")
    val objectKey: String,
    @field:Schema(description = "생성된 Attachment ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val attachmentId: UUID,
) {
    companion object {
        fun from(
            attachment: Attachment,
            presignedUrl: String,
        ): PresignedUrlResponse =
            PresignedUrlResponse(
                presignedUrl = presignedUrl,
                objectKey = attachment.objectKey,
                attachmentId = attachment.id!!,
            )
    }
}
