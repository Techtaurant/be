package com.techtaurant.mainserver.attachment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.util.UUID

@Schema(description = "임시 첨부파일 미리보기 URL 일괄 발급 요청")
data class AttachmentPreviewUrlsRequest(
    @field:NotEmpty
    @field:Schema(
        description = "미리보기할 TMP Attachment ID 목록",
        example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"660e8400-e29b-41d4-a716-446655440000\"]",
    )
    val attachmentIds: List<UUID>,
)
