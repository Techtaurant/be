package com.techtaurant.mainserver.attachment.dto

import com.techtaurant.mainserver.attachment.entity.Attachment
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "임시 첨부파일 미리보기 URL 응답")
data class AttachmentPreviewUrlResponse(
    @field:Schema(description = "첨부파일 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val attachmentId: UUID,
    @field:Schema(description = "S3 오브젝트 키", example = "tmp/550e8400-e29b-41d4-a716-446655440000/photo.jpg")
    val objectKey: String,
    @field:Schema(description = "S3 GET Presigned URL", example = "https://techtaurant-media.s3.ap-northeast-2.amazonaws.com/tmp/...")
    val presignedUrl: String,
) {
    companion object {
        fun from(
            attachment: Attachment,
            presignedUrl: String,
        ): AttachmentPreviewUrlResponse =
            AttachmentPreviewUrlResponse(
                attachmentId = attachment.id!!,
                objectKey = attachment.objectKey,
                presignedUrl = presignedUrl,
            )
    }
}
