package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.notification.dto.NotificationCursor
import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.entity.NotificationTarget
import com.techtaurant.mainserver.notification.enums.NotificationTargetRole
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationRecipientRepository
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationTargetRepository
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
    private val notificationTargetRepository: NotificationTargetRepository,
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
                notificationRecipientRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(
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

        val notificationIds = content.map { it.notification.id!! }
        val targetsByNotificationId =
            if (notificationIds.isEmpty()) {
                emptyMap()
            } else {
                notificationTargetRepository.findAllByNotificationIdInOrderByCreatedAtAsc(notificationIds)
                    .groupBy { it.notification.id!! }
            }
        val renderContexts = buildRenderContexts(targetsByNotificationId)

        return CursorPageResponse(
            content =
                content.map { recipient ->
                    val notification = recipient.notification
                    val notificationId = notification.id!!
                    val targets = targetsByNotificationId[notificationId].orEmpty()
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
                        targets = targets,
                    )
                },
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    @Transactional
    fun markNotificationsRead(
        userId: UUID,
        notificationIds: List<UUID>,
    ) {
        val distinctNotificationIds = notificationIds.distinct()
        if (distinctNotificationIds.isEmpty()) {
            return
        }

        val unreadRecipients =
            notificationRecipientRepository.findAllByUserIdAndNotificationIdInAndReadAtIsNull(
                userId = userId,
                notificationIds = distinctNotificationIds,
            )

        if (unreadRecipients.isEmpty()) {
            return
        }

        val readAt = Date()
        unreadRecipients.forEach { recipient ->
            recipient.markAsRead(readAt)
        }
    }

    private fun buildRenderContexts(targetsByNotificationId: Map<UUID, List<NotificationTarget>>): Map<UUID, NotificationRenderContext> {
        if (targetsByNotificationId.isEmpty()) {
            return emptyMap()
        }

        val flattenedTargets = targetsByNotificationId.values.flatten()
        val actorUserIds =
            flattenedTargets
                .filter { it.role == NotificationTargetRole.ACTOR && it.targetType == NotificationTargetType.USER }
                .map { it.targetId }
                .distinct()
        val actorsById = findUsersById(actorUserIds)
        val actorProfileImageUrlByUserId = userProfileImageResolver.resolve(actorsById.values.toList())

        val postIds =
            flattenedTargets
                .filter { it.targetType == NotificationTargetType.POST }
                .map { it.targetId }
                .distinct()
        val postsById = findPostsById(postIds)
        val postThumbnailUrlByPostId = resolvePostThumbnailUrlByPostId(postsById)

        return targetsByNotificationId.mapValues { (_, targets) ->
            val actorUserId =
                targets.firstOrNull {
                    it.role == NotificationTargetRole.ACTOR && it.targetType == NotificationTargetType.USER
                }?.targetId
            val postId = targets.firstOrNull { it.targetType == NotificationTargetType.POST }?.targetId
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
