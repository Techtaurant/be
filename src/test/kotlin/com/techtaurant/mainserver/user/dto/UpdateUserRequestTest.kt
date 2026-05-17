package com.techtaurant.mainserver.user.dto

import com.techtaurant.mainserver.common.exception.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class UpdateUserRequestTest {
    @Test
    @DisplayName("서비스 프로필 이미지 attachment ID가 null이면 변경 대상으로 보지 않는다")
    fun hasServiceProfileImageAttachmentId_null_returnsFalse() {
        // given
        val request = UpdateUserRequest(serviceProfileImageAttachmentId = null)

        // when
        val hasAttachmentId = request.hasServiceProfileImageAttachmentId()
        val parsedAttachmentId = request.parseServiceProfileImageAttachmentId()

        // then
        assertThat(hasAttachmentId).isFalse()
        assertThat(parsedAttachmentId).isNull()
    }

    @Test
    @DisplayName("서비스 프로필 이미지 attachment ID가 UUID 문자열이면 파싱한다")
    fun parseServiceProfileImageAttachmentId_validUuid_returnsUuid() {
        // given
        val attachmentId = UUID.randomUUID()
        val request = UpdateUserRequest(serviceProfileImageAttachmentId = attachmentId.toString())

        // when
        val parsedAttachmentId = request.parseServiceProfileImageAttachmentId()

        // then
        assertThat(parsedAttachmentId).isEqualTo(attachmentId)
    }

    @Test
    @DisplayName("서비스 프로필 이미지 attachment ID가 UUID 형식이 아니면 예외를 던진다")
    fun parseServiceProfileImageAttachmentId_invalidUuid_throwsBadRequest() {
        // given
        val request = UpdateUserRequest(serviceProfileImageAttachmentId = "invalid-uuid")

        // when then
        assertThatThrownBy { request.parseServiceProfileImageAttachmentId() }
            .isInstanceOf(ApiException::class.java)
            .hasMessage("serviceProfileImageAttachmentId는 UUID 형식이어야 합니다")
    }
}
