package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.entity.UserProfileImageSource
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserProfileImageResolver(
    private val attachmentService: AttachmentService,
) {
    fun resolve(user: User): String {
        val userId = user.id ?: return user.getFallbackProfileImageUrl()
        return resolve(listOf(user))[userId] ?: user.getFallbackProfileImageUrl()
    }

    fun resolve(users: List<User>): Map<UUID, String> {
        val persistedUsers = users.filter { it.id != null }
        if (persistedUsers.isEmpty()) {
            return emptyMap()
        }

        val userIds = persistedUsers.mapNotNull { it.id }
        val attachmentsByUserId =
            attachmentService.getConfirmedAttachmentsByReferenceIds(userIds, AttachmentReferenceType.USER)
        val presignedUrlByAttachmentId =
            attachmentService.generatePresignedDownloadUrlMapByAttachments(attachmentsByUserId.values.flatten())

        return persistedUsers.associate { user ->
            user.id!! to resolve(user, presignedUrlByAttachmentId)
        }
    }

    private fun resolve(
        user: User,
        presignedUrlByAttachmentId: Map<UUID, String>,
    ): String =
        when (val source = user.getProfileImageSource()) {
            is UserProfileImageSource.ServiceAttachment ->
                presignedUrlByAttachmentId[source.attachmentId] ?: user.getFallbackProfileImageUrl()
            is UserProfileImageSource.Url -> source.url
        }
}
