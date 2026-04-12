package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.application.BannedUserMaskingService
import com.techtaurant.mainserver.user.application.UserBanService
import com.techtaurant.mainserver.user.application.UserProfileImageResolver
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class CommentResponseAssemblerTest {
    private val userBanService: UserBanService = mockk()
    private val bannedUserMaskingService: BannedUserMaskingService = mockk()
    private val attachmentService: AttachmentService = mockk()
    private val userProfileImageResolver = UserProfileImageResolver(attachmentService)
    private val commentResponseAssembler =
        CommentResponseAssembler(
            userBanService = userBanService,
            bannedUserMaskingService = bannedUserMaskingService,
            userProfileImageResolver = userProfileImageResolver,
        )

    @Test
    @DisplayName("서비스 프로필 이미지 attachment가 있으면 댓글 작성자 프로필 이미지에 presigned URL을 사용한다")
    fun assemble_serviceProfileImageAttachment_usesPresignedAuthorProfileImageUrl() {
        val authorId = UUID.randomUUID()
        val attachmentId = UUID.randomUUID()
        val author =
            User(
                name = "댓글 작성자",
                email = "comment@author.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "comment-author",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/fallback-comment-author.jpg",
                serviceProfileImageAttachmentId = attachmentId,
            ).apply { id = authorId }
        val post =
            Post(
                title = "게시물",
                content = "본문",
                author = author,
            ).apply { id = UUID.randomUUID() }
        val comment =
            Comment(
                content = "댓글",
                post = post,
                author = author,
            ).apply { id = UUID.randomUUID() }
        val profileAttachment = createUserProfileAttachment(authorId, attachmentId)

        every { userBanService.getBannedUserIds(null) } returns emptySet()
        every {
            attachmentService.getConfirmedAttachmentsByReferenceIds(listOf(authorId), AttachmentReferenceType.USER)
        } returns mapOf(authorId to listOf(profileAttachment))
        every {
            attachmentService.generatePresignedDownloadUrlMapByAttachments(listOf(profileAttachment))
        } returns mapOf(attachmentId to "https://cdn.example.com/comments/author-profile.png")

        val result = commentResponseAssembler.assemble(listOf(comment), emptyMap(), null)

        assertThat(result.single().authorProfileImageUrl)
            .isEqualTo("https://cdn.example.com/comments/author-profile.png")
    }

    private fun createUserProfileAttachment(
        userId: UUID,
        attachmentId: UUID,
    ): Attachment =
        Attachment(
            referenceId = userId,
            referenceType = AttachmentReferenceType.USER,
            objectKey = "users/$userId/$attachmentId/profile.png",
            status = AttachmentStatus.CONFIRMED,
            originalFileName = "profile.png",
            contentType = "image/png",
            fileSize = 1024L,
        ).apply { id = attachmentId }
}
