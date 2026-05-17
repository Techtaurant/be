package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "회사 등록 요청")
data class CreateCompanyRequest(
    @field:NotBlank(message = "회사 이름은 필수입니다")
    @field:Size(max = 255, message = "회사 이름은 최대 255자까지 가능합니다")
    @field:Schema(description = "회사명", example = "토스")
    val name: String,
    @field:NotBlank(message = "회사 이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    @field:Schema(description = "회사 대표 이메일", example = "contact@toss.im")
    val email: String,
    @field:Schema(description = "회사 프로필 이미지 URL", example = "https://static.toss.im/logo.png", nullable = true)
    val profileImageUrl: String? = null,
    @field:Schema(
        description = "서비스에 등록한 회사 프로필 이미지 attachment ID. null이면 외부 URL 또는 빈 값이 사용됩니다.",
        example = "01234567-89ab-cdef-0123-456789abcdef",
        nullable = true,
    )
    val serviceProfileImageAttachmentId: String? = null,
) {
    fun parseServiceProfileImageAttachmentId(): UUID? {
        return serviceProfileImageAttachmentId?.let { rawValue ->
            runCatching { UUID.fromString(rawValue) }
                .getOrElse {
                    throw ApiException(DefaultStatus.BAD_REQUEST, "serviceProfileImageAttachmentId는 UUID 형식이어야 합니다")
                }
        }
    }
}
