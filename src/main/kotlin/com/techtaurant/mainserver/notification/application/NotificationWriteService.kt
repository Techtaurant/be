package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.notification.entity.Notification
import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import com.techtaurant.mainserver.notification.entity.NotificationTarget
import com.techtaurant.mainserver.notification.enums.NotificationStatus
import com.techtaurant.mainserver.notification.enums.NotificationTargetRole
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationRepository
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.UUID

@Service
class NotificationWriteService(
    private val notificationRepository: NotificationRepository,
    private val notificationPayloadService: NotificationPayloadService,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
) {
    @Transactional
    fun createPostCommentNotification(
        actorUserId: UUID,
        recipientUserId: UUID,
        postId: UUID,
        commentId: UUID,
        locale: Locale? = null,
    ): UUID {
        val actor = resolveActor(actorUserId)
        val recipients = resolveRecipients(listOf(recipientUserId))
        val post = resolvePost(postId)

        resolveComment(commentId)

        return createNotification(
            type = NotificationType.POST_COMMENT,
            payloadHtml = notificationPayloadService.buildPostCommentPayload(actor.name, post.title, locale),
            recipients = recipients,
            targetSpecs =
                listOf(
                    NotificationTargetSpec(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actor.id!!),
                    NotificationTargetSpec(NotificationTargetRole.TARGET, NotificationTargetType.POST, postId),
                    NotificationTargetSpec(NotificationTargetRole.TARGET, NotificationTargetType.COMMENT, commentId),
                ),
        )
    }

    @Transactional
    fun createCommentReplyNotification(
        actorUserId: UUID,
        recipientUserId: UUID,
        postId: UUID,
        commentId: UUID,
        locale: Locale? = null,
    ): UUID {
        val actor = resolveActor(actorUserId)
        val recipients = resolveRecipients(listOf(recipientUserId))
        val post = resolvePost(postId)

        resolveComment(commentId)

        return createNotification(
            type = NotificationType.COMMENT_REPLY,
            payloadHtml = notificationPayloadService.buildCommentReplyPayload(actor.name, post.title, locale),
            recipients = recipients,
            targetSpecs =
                listOf(
                    NotificationTargetSpec(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actor.id!!),
                    NotificationTargetSpec(NotificationTargetRole.TARGET, NotificationTargetType.POST, postId),
                    NotificationTargetSpec(NotificationTargetRole.TARGET, NotificationTargetType.COMMENT, commentId),
                ),
        )
    }

    @Transactional
    fun createFollowerPostNotification(
        actorUserId: UUID,
        recipientUserIds: List<UUID>,
        postId: UUID,
        locale: Locale? = null,
    ): UUID {
        val actor = resolveActor(actorUserId)
        val recipients = resolveRecipients(recipientUserIds)
        val post = resolvePost(postId)

        return createNotification(
            type = NotificationType.FOLLOWER_POST,
            payloadHtml = notificationPayloadService.buildFollowerPostPayload(actor.name, post.title, locale),
            recipients = recipients,
            targetSpecs =
                listOf(
                    NotificationTargetSpec(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actor.id!!),
                    NotificationTargetSpec(NotificationTargetRole.TARGET, NotificationTargetType.POST, postId),
                ),
        )
    }

    @Transactional
    fun createFollowNotification(
        actorUserId: UUID,
        recipientUserId: UUID,
        locale: Locale? = null,
    ): UUID {
        val actor = resolveActor(actorUserId)
        val recipients = resolveRecipients(listOf(recipientUserId))

        return createNotification(
            type = NotificationType.FOLLOW,
            payloadHtml = notificationPayloadService.buildFollowPayload(actor.name, locale),
            recipients = recipients,
            targetSpecs =
                listOf(
                    NotificationTargetSpec(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actor.id!!),
                    NotificationTargetSpec(NotificationTargetRole.TARGET, NotificationTargetType.USER, actor.id!!),
                ),
        )
    }

    private fun createNotification(
        type: NotificationType,
        payloadHtml: String,
        recipients: List<User>,
        targetSpecs: List<NotificationTargetSpec>,
    ): UUID {
        if (targetSpecs.isEmpty()) {
            throw ApiException(NotificationStatus.NOTIFICATION_TARGET_REQUIRED)
        }

        val notification =
            Notification(
                type = type,
                payloadHtml = requirePayload(payloadHtml),
            )

        targetSpecs.distinct().forEach { spec ->
            notification.addTarget(
                NotificationTarget(
                    notification = notification,
                    role = spec.role,
                    targetType = spec.targetType,
                    targetId = spec.targetId,
                ),
            )
        }

        recipients.distinctBy { it.id }.forEach { recipient ->
            notification.addRecipient(
                NotificationRecipient(
                    notification = notification,
                    user = recipient,
                ),
            )
        }

        return notificationRepository.save(notification).id!!
    }

    private fun requirePayload(payloadHtml: String): String {
        if (payloadHtml.isBlank()) {
            throw ApiException(NotificationStatus.NOTIFICATION_PAYLOAD_REQUIRED)
        }

        return payloadHtml
    }

    private fun resolveActor(actorUserId: UUID): User {
        return userRepository.findById(actorUserId).orElseThrow {
            ApiException(UserStatus.ID_NOT_FOUND)
        }
    }

    private fun resolveRecipients(recipientUserIds: List<UUID>): List<User> {
        val distinctRecipientIds = recipientUserIds.distinct()
        if (distinctRecipientIds.isEmpty()) {
            throw ApiException(NotificationStatus.NOTIFICATION_RECIPIENT_REQUIRED)
        }

        val recipients = userRepository.findAllById(distinctRecipientIds)
        if (recipients.size != distinctRecipientIds.size) {
            throw ApiException(UserStatus.USER_NOT_FOUND)
        }

        val recipientsById = recipients.associateBy { it.id!! }
        return distinctRecipientIds.map { recipientId -> recipientsById.getValue(recipientId) }
    }

    private fun resolvePost(postId: UUID): Post {
        return postRepository.findById(postId).orElseThrow {
            ApiException(PostStatus.POST_NOT_FOUND)
        }
    }

    private fun resolveComment(commentId: UUID): Comment {
        return commentRepository.findById(commentId).orElseThrow {
            ApiException(CommentStatus.COMMENT_NOT_FOUND)
        }
    }

    private data class NotificationTargetSpec(
        val role: NotificationTargetRole,
        val targetType: NotificationTargetType,
        val targetId: UUID,
    )
}
