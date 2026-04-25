package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.enums.NotificationType
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.user.entity.User
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class FollowerPostNotificationPayloadStrategy(
    notificationPayloadService: NotificationPayloadService,
    notificationPayloadResourceResolver: NotificationPayloadResourceResolver,
) : AbstractActorPostNotificationPayloadStrategy(
        notificationPayloadService = notificationPayloadService,
        notificationPayloadResourceResolver = notificationPayloadResourceResolver,
    ) {
    override val type: NotificationType = NotificationType.FOLLOWER_POST
    override val messageKey: String = "notification.payload.follower-post"

    override fun createMedia(
        actor: User?,
        post: Post?,
        actorProfileImageUrlByUserId: Map<UUID, String>,
        postThumbnailUrlByPostId: Map<UUID, String>,
    ): NotificationPayloadService.NotificationPayloadMedia = createPostThumbnailMedia(post, postThumbnailUrlByPostId)
}
