package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.entity.UserProfileImageSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserProfileImageResolver(
    private val attachmentService: AttachmentService,
    @param:Value("\${app.default-user-thumbnail-url:/static/images/user-thumbnail.png}")
    private val defaultUserThumbnailUrl: String = "/static/images/user-thumbnail.png",
    @param:Value("\${swagger.base-url:http://localhost:8080}")
    private val baseUrl: String = "http://localhost:8080",
) {
    fun resolve(user: User): String {
        val userId = user.id ?: return fallbackProfileImageUrl(user)
        return resolve(listOf(user))[userId] ?: fallbackProfileImageUrl(user)
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
                presignedUrlByAttachmentId[source.attachmentId] ?: fallbackProfileImageUrl(user)
            is UserProfileImageSource.Url -> normalizeProfileImageUrl(source.url)
        }

    private fun fallbackProfileImageUrl(user: User): String = normalizeProfileImageUrl(user.getFallbackProfileImageUrl())

    private fun normalizeProfileImageUrl(url: String): String {
        val candidate = url.trim()
        if (candidate.isBlank()) {
            return defaultUserThumbnailUrl()
        }

        return absoluteUrl(candidate)
    }

    private fun defaultUserThumbnailUrl(): String = absoluteUrl(defaultUserThumbnailUrl)

    private fun absoluteUrl(url: String): String {
        return when {
            url.startsWith("http://") -> url
            url.startsWith("https://") -> url
            url.startsWith("/") -> "${baseUrl.trimEnd('/')}$url"
            else -> url
        }
    }
}
