package com.techtaurant.mainserver.attachment.dto

import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

@Schema(description = "Presigned URL 발급 요청")
data class PresignedUrlRequest(
    @field:Schema(description = "업로드할 파일명", example = "photo.jpg")
    @field:NotBlank
    val fileName: String,
    @field:Schema(description = "파일 MIME 타입", example = "image/jpeg")
    @field:NotBlank
    val contentType: String,
    @field:Schema(description = "파일 크기 (bytes)", example = "1048576")
    @field:Positive
    val fileSize: Long,
    @field:Schema(description = "첨부파일 연관 도메인 타입", example = "POST")
    val referenceType: AttachmentReferenceType,
)
