package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.enums.NotificationType
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.user.entity.User
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class CommentReplyNotificationListItemRenderStrategy(
    notificationPayloadService: NotificationPayloadService,
    notificationPayloadResourceResolver: NotificationPayloadResourceResolver,
) : AbstractActorPostNotificationListItemRenderStrategy(
        notificationPayloadService = notificationPayloadService,
        notificationPayloadResourceResolver = notificationPayloadResourceResolver,
    ) {
    override val type: NotificationType = NotificationType.COMMENT_REPLY
    override val messageKey: String = "notification.payload.comment-reply"

    override fun selectThumbnailMedia(
        actor: User?,
        post: Post?,
        actorProfileImageUrlByUserId: Map<UUID, String>,
        postThumbnailUrlByPostId: Map<UUID, String>,
    ): NotificationPayloadService.NotificationPayloadMedia = createActorProfileMedia(actor, actorProfileImageUrlByUserId)
}
