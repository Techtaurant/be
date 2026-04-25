package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.notification.dto.NotificationCursor
import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.entity.NotificationArgument
import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationRecipientRepository
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationArgumentRepository
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.user.application.UserProfileImageResolver
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.UUID

@Service
class NotificationReadService(
    private val notificationRecipientRepository: NotificationRecipientRepository,
    private val notificationArgumentRepository: NotificationArgumentRepository,
    private val notificationPayloadService: NotificationPayloadService,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val attachmentService: AttachmentService,
    private val userProfileImageResolver: UserProfileImageResolver,
    @param:Value("\${app.default-post-thumbnail-url}")
    private val defaultPostThumbnailUrl: String,
    @param:Value("\${app.default-user-thumbnail-url:/static/images/user-thumbnail.png}")
    private val defaultUserThumbnailUrl: String,
    @param:Value("\${swagger.base-url}")
    private val baseUrl: String,
) {
    @Transactional(readOnly = true)
    fun getMyNotifications(
        userId: UUID,
        cursor: String?,
        size: Int,
    ): CursorPageResponse<NotificationListItemResponse> {
        val notificationCursor = cursor?.let { NotificationCursor.decode(it) }

        if (cursor != null && notificationCursor == null) {
            return CursorPageResponse(
                content = emptyList(),
                nextCursor = null,
                hasNext = false,
                size = 0,
            )
        }

        val recipients =
            if (notificationCursor == null) {
                notificationRecipientRepository.findAllByRecipientUserIdOrderByCreatedAtDescIdDesc(
                    userId = userId,
                    pageable = PageRequest.of(0, size + 1),
                )
            } else {
                notificationRecipientRepository.findPageByUserIdAndCursor(
                    userId = userId,
                    cursorCreatedAt = notificationCursor.createdAt,
                    cursorId = notificationCursor.id,
                    pageable = PageRequest.of(0, size + 1),
                )
            }

        val hasNext = recipients.size > size
        val content = recipients.take(size)
        val nextCursor =
            if (hasNext && content.isNotEmpty()) {
                NotificationCursor.from(content.last()).encode()
            } else {
                null
            }

        return CursorPageResponse(
            content = buildNotificationListItems(content),
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    @Transactional
    fun markNotificationsRead(
        userId: UUID,
        notificationIds: List<UUID>,
    ): List<NotificationListItemResponse> {
        val distinctNotificationIds = notificationIds.distinct()
        if (distinctNotificationIds.isEmpty()) {
            return emptyList()
        }

        val unreadRecipients =
            notificationRecipientRepository.findAllByRecipientUserIdAndNotificationIdInAndReadAtIsNull(
                userId = userId,
                notificationIds = distinctNotificationIds,
            )

        if (unreadRecipients.isEmpty()) {
            return emptyList()
        }

        val unreadRecipientsByNotificationId = unreadRecipients.associateBy { it.notification.id!! }
        val updatedRecipients = distinctNotificationIds.mapNotNull(unreadRecipientsByNotificationId::get)
        val readAt = Date()
        updatedRecipients.forEach { recipient ->
            recipient.markAsRead(readAt)
        }

        return buildNotificationListItems(updatedRecipients)
    }

    private fun buildNotificationListItems(recipients: List<NotificationRecipient>): List<NotificationListItemResponse> {
        if (recipients.isEmpty()) {
            return emptyList()
        }

        val notificationIds = recipients.map { it.notification.id!! }
        val rawArgumentsByNotificationId =
            notificationArgumentRepository.findAllByNotificationIdInOrderByCreatedAtAsc(notificationIds)
                .groupBy { it.notification.id!! }
        val argumentsByNotificationId =
            recipients.associate { recipient ->
                val notification = recipient.notification
                val notificationId = notification.id!!
                notificationId to
                    normalizeArguments(
                        type = notification.type,
                        recipientUserId = recipient.recipientUser.id!!,
                        arguments = rawArgumentsByNotificationId[notificationId].orEmpty(),
                    )
            }
        val renderContexts = buildRenderContexts(argumentsByNotificationId)

        return recipients.map { recipient ->
            val notification = recipient.notification
            val notificationId = notification.id!!
            val arguments = argumentsByNotificationId[notificationId].orEmpty()
            val renderContext = renderContexts[notificationId] ?: NotificationRenderContext.EMPTY

            NotificationListItemResponse.from(
                recipient = recipient,
                payloadHtml =
                    notificationPayloadService.buildPayload(
                        type = notification.type,
                        actorName = renderContext.actorName,
                        postTitle = renderContext.postTitle,
                        media = resolvePayloadMedia(notification.type, renderContext),
                    ),
                arguments = arguments,
            )
        }
    }

    private fun normalizeArguments(
        type: NotificationType,
        recipientUserId: UUID,
        arguments: List<NotificationArgument>,
    ): List<NotificationArgument> =
        if (type == NotificationType.FOLLOW) {
            arguments.filterNot { it.targetType == NotificationTargetType.USER && it.targetId == recipientUserId }
        } else {
            arguments
        }

    private fun buildRenderContexts(argumentsByNotificationId: Map<UUID, List<NotificationArgument>>): Map<UUID, NotificationRenderContext> {
        if (argumentsByNotificationId.isEmpty()) {
            return emptyMap()
        }

        val flattenedArguments = argumentsByNotificationId.values.flatten()
        val actorUserIds =
            flattenedArguments
                .filter { it.targetType == NotificationTargetType.USER }
                .map { it.targetId }
                .distinct()
        val actorsById = findUsersById(actorUserIds)
        val actorProfileImageUrlByUserId = userProfileImageResolver.resolve(actorsById.values.toList())

        val postIds =
            flattenedArguments
                .filter { it.targetType == NotificationTargetType.POST }
                .map { it.targetId }
                .distinct()
        val postsById = findPostsById(postIds)
        val postThumbnailUrlByPostId = resolvePostThumbnailUrlByPostId(postsById)

        return argumentsByNotificationId.mapValues { (_, arguments) ->
            val actorUserId = arguments.firstOrNull { it.targetType == NotificationTargetType.USER }?.targetId
            val postId = arguments.firstOrNull { it.targetType == NotificationTargetType.POST }?.targetId
            val actor = actorUserId?.let(actorsById::get)
            val post = postId?.let(postsById::get)

            NotificationRenderContext(
                actorName = actor?.name.orEmpty(),
                actorProfileImageUrl = resolveActorProfileImageUrl(actor, actorProfileImageUrlByUserId),
                postTitle = post?.title,
                postThumbnailUrl = post?.id?.let(postThumbnailUrlByPostId::get) ?: absoluteUrl(defaultPostThumbnailUrl),
            )
        }
    }

    private fun findUsersById(userIds: List<UUID>): Map<UUID, User> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }

        return userRepository.findAllById(userIds).associateBy { it.id!! }
    }

    private fun findPostsById(postIds: List<UUID>): Map<UUID, Post> {
        if (postIds.isEmpty()) {
            return emptyMap()
        }

        return postRepository.findAllById(postIds).associateBy { it.id!! }
    }

    private fun resolveActorProfileImageUrl(
        actor: User?,
        actorProfileImageUrlByUserId: Map<UUID, String>,
    ): String {
        val resolvedImageUrl = actor?.id?.let(actorProfileImageUrlByUserId::get)
        return absoluteUrl(resolvedImageUrl).ifBlank { absoluteUrl(defaultUserThumbnailUrl) }
    }

    private fun resolvePostThumbnailUrlByPostId(postsById: Map<UUID, Post>): Map<UUID, String> {
        if (postsById.isEmpty()) {
            return emptyMap()
        }

        val postIds = postsById.keys.toList()
        val attachmentsByPostId =
            attachmentService.getConfirmedAttachmentsByReferenceIds(postIds, AttachmentReferenceType.POST)
        val thumbnailAttachmentByPostId =
            postsById.values.associate { post ->
                val postId = post.id!!
                val attachments = attachmentsByPostId[postId].orEmpty()
                val thumbnailAttachment =
                    post.thumbnailImage?.let { thumbnailAttachmentId ->
                        attachments.firstOrNull { it.id == thumbnailAttachmentId }
                    } ?: attachments.minByOrNull { it.createdAt }

                postId to thumbnailAttachment
            }
        val presignedThumbnailUrlByAttachmentId =
            thumbnailAttachmentByPostId.values
                .filterNotNull()
                .takeIf { it.isNotEmpty() }
                ?.let { attachmentService.generatePresignedDownloadUrlMapByAttachments(it) }
                ?: emptyMap()

        return postsById.values.associate { post ->
            val postId = post.id!!
            val thumbnailUrl =
                thumbnailAttachmentByPostId[postId]
                    ?.id
                    ?.let { attachmentId -> presignedThumbnailUrlByAttachmentId[attachmentId] }
                    ?: absoluteUrl(defaultPostThumbnailUrl)
            postId to thumbnailUrl
        }
    }

    private fun resolvePayloadMedia(
        type: NotificationType,
        renderContext: NotificationRenderContext,
    ): NotificationPayloadService.NotificationPayloadMedia =
        when (type) {
            NotificationType.FOLLOWER_POST ->
                NotificationPayloadService.NotificationPayloadMedia(
                    url = renderContext.postThumbnailUrl,
                    alt = renderContext.postTitle?.let { "$it 썸네일" } ?: "게시물 썸네일",
                )
            NotificationType.POST_COMMENT,
            NotificationType.COMMENT_REPLY,
            NotificationType.FOLLOW ->
                NotificationPayloadService.NotificationPayloadMedia(
                    url = renderContext.actorProfileImageUrl,
                    alt = renderContext.actorName.takeIf { it.isNotBlank() }?.let { "$it 프로필 이미지" } ?: "사용자 프로필 이미지",
                )
        }

    private fun absoluteUrl(url: String?): String {
        val candidate = url?.trim().orEmpty()
        if (candidate.isBlank()) {
            return candidate
        }

        return when {
            candidate.startsWith("http://") -> candidate
            candidate.startsWith("https://") -> candidate
            candidate.startsWith("/") -> "${baseUrl.trimEnd('/')}$candidate"
            else -> candidate
        }
    }

    private data class NotificationRenderContext(
        val actorName: String,
        val actorProfileImageUrl: String,
        val postTitle: String?,
        val postThumbnailUrl: String,
    ) {
        companion object {
            val EMPTY =
                NotificationRenderContext(
                    actorName = "",
                    actorProfileImageUrl = "",
                    postTitle = null,
                    postThumbnailUrl = "",
                )
        }
    }
}
