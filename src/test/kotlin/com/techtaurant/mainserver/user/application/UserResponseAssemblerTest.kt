package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class UserResponseAssemblerTest {
    private val attachmentService: AttachmentService = mockk()
    private val userProfileImageResolver = UserProfileImageResolver(attachmentService)
    private val userResponseAssembler = UserResponseAssembler(userProfileImageResolver)

    @Test
    @DisplayName("서비스 프로필 이미지 attachment가 있으면 presigned URL을 우선 반환한다")
    fun assemble_serviceProfileImageAttachment_usesPresignedUrl() {
        val userId = UUID.randomUUID()
        val attachmentId = UUID.randomUUID()
        val user = createUser(userId, attachmentId)
        val attachment = createAttachment(userId, attachmentId)

        every {
            attachmentService.getConfirmedAttachmentsByReferenceIds(listOf(userId), AttachmentReferenceType.USER)
        } returns mapOf(userId to listOf(attachment))
        every {
            attachmentService.generatePresignedDownloadUrlMapByAttachments(listOf(attachment))
        } returns mapOf(attachmentId to "https://cdn.example.com/profile.png")

        val response = userResponseAssembler.assemble(user)

        assertThat(response.profileImageUrl).isEqualTo("https://cdn.example.com/profile.png")
    }

    @Test
    @DisplayName("서비스 프로필 이미지 attachment가 없으면 기본 profileImageUrl을 사용한다")
    fun assemble_withoutServiceProfileImage_usesFallbackUrl() {
        val userId = UUID.randomUUID()
        val user = createUser(userId, null)

        every {
            attachmentService.getConfirmedAttachmentsByReferenceIds(listOf(userId), AttachmentReferenceType.USER)
        } returns emptyMap()
        every {
            attachmentService.generatePresignedDownloadUrlMapByAttachments(emptyList())
        } returns emptyMap()

        val response = userResponseAssembler.assemble(user)

        assertThat(response.profileImageUrl).isEqualTo("https://example.com/default-profile.jpg")
    }

    private fun createUser(
        userId: UUID,
        attachmentId: UUID?,
    ): User {
        return User(
            name = "테스트사용자",
            email = "user@example.com",
            provider = OAuthProvider.GOOGLE,
            identifier = "google-${UUID.randomUUID()}",
            role = UserRole.USER,
            profileImageUrl = "https://example.com/default-profile.jpg",
            serviceProfileImageAttachmentId = attachmentId,
        ).apply { id = userId }
    }

    private fun createAttachment(
        userId: UUID,
        attachmentId: UUID,
    ): Attachment {
        return Attachment(
            referenceId = userId,
            referenceType = AttachmentReferenceType.USER,
            objectKey = "users/$userId/$attachmentId/profile.png",
            status = AttachmentStatus.CONFIRMED,
            originalFileName = "profile.png",
            contentType = "image/png",
            fileSize = 1024L,
        ).apply { id = attachmentId }
    }
}
