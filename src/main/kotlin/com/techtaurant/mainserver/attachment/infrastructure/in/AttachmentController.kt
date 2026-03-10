package com.techtaurant.mainserver.attachment.infrastructure.`in`

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.dto.PresignedUrlRequest
import com.techtaurant.mainserver.attachment.dto.PresignedUrlResponse
import com.techtaurant.mainserver.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
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
    @PostMapping("/presigned-url")
    override fun issuePresignedUploadUrl(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: PresignedUrlRequest,
    ): ApiResponse<PresignedUrlResponse> = ApiResponse.ok(attachmentService.issuePresignedUploadUrl(request))
}
