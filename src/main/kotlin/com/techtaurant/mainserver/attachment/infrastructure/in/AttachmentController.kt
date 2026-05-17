package com.techtaurant.mainserver.attachment.infrastructure.`in`

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.dto.AttachmentPreviewUrlResponse
import com.techtaurant.mainserver.attachment.dto.AttachmentPreviewUrlsRequest
import com.techtaurant.mainserver.attachment.dto.PresignedUrlRequest
import com.techtaurant.mainserver.attachment.dto.PresignedUrlResponse
import com.techtaurant.mainserver.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/attachments")
@Validated
class AttachmentController(
    private val attachmentService: AttachmentService,
) : AttachmentControllerDocs {
    @PostMapping("/preview-urls")
    override fun issueTmpPreviewUrls(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: AttachmentPreviewUrlsRequest,
    ): ApiResponse<List<AttachmentPreviewUrlResponse>> = ApiResponse.ok(attachmentService.issueTmpPreviewUrls(request))

    @GetMapping("/{attachmentId}/preview-url")
    override fun issueTmpPreviewUrl(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable attachmentId: UUID,
    ): ApiResponse<AttachmentPreviewUrlResponse> = ApiResponse.ok(attachmentService.issueTmpPreviewUrl(attachmentId))

    @PostMapping("/presigned-url")
    override fun issuePresignedUploadUrl(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: PresignedUrlRequest,
    ): ApiResponse<PresignedUrlResponse> = ApiResponse.ok(attachmentService.issuePresignedUploadUrl(request))
}
