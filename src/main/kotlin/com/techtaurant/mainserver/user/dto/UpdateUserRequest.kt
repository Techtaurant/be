package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "사용자 정보 수정 요청")
data class UpdateUserRequest(
    @field:Size(max = 255, message = "이름은 최대 255자까지 가능합니다")
    @field:Schema(description = "수정할 이름", example = "테크식당")
    val name: String? = null,
    @field:Schema(
        description = "서비스에 등록한 프로필 이미지 attachment ID. null이거나 생략하면 변경하지 않습니다.",
        example = "01234567-89ab-cdef-0123-456789abcdef",
        nullable = true,
    )
    val serviceProfileImageAttachmentId: String? = null,
) {
    fun hasServiceProfileImageAttachmentId(): Boolean {
        return serviceProfileImageAttachmentId != null
    }

    fun parseServiceProfileImageAttachmentId(): UUID? {
        return serviceProfileImageAttachmentId?.let { rawValue ->
            runCatching { UUID.fromString(rawValue) }
                .getOrElse {
                    throw ApiException(DefaultStatus.BAD_REQUEST, "serviceProfileImageAttachmentId는 UUID 형식이어야 합니다")
                }
        }
    }
}
