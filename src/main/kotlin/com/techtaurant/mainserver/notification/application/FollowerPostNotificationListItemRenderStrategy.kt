package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import com.techtaurant.mainserver.post.entity.Post
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class FollowerPostNotificationListItemRenderStrategy(
    private val notificationPayloadService: NotificationPayloadService,
    private val notificationPayloadResourceResolver: NotificationPayloadResourceResolver,
) : NotificationListItemRenderStrategy {
    override val type: NotificationType = NotificationType.FOLLOWER_POST

    override fun render(commands: List<NotificationListItemRenderCommand>): Map<UUID, NotificationListItemResponse> {
        if (commands.isEmpty()) {
            return emptyMap()
        }

        val actorUserIds = commands.mapNotNull { it.arguments.findTargetId(NotificationTargetType.USER) }
        val actorsById = notificationPayloadResourceResolver.findUsersById(actorUserIds)

        val postIds = commands.mapNotNull { it.arguments.findTargetId(NotificationTargetType.POST) }
        val postsById = notificationPayloadResourceResolver.findPostsById(postIds)
        val postThumbnailUrlByPostId =
            notificationPayloadResourceResolver.resolvePostThumbnailUrlByPostId(postsById)

        return commands.associate { command ->
            val recipient = command.recipient
            val notificationId = recipient.notification.id!!
            val actor = command.arguments.findTargetId(NotificationTargetType.USER)?.let(actorsById::get)
            val post = command.arguments.findTargetId(NotificationTargetType.POST)?.let(postsById::get)
            val media = createMedia(post, postThumbnailUrlByPostId)

            notificationId to
                NotificationListItemResponse.from(
                    recipient = recipient,
                    payloadHtml =
                        notificationPayloadService.buildPayload(
                            messageKey = MESSAGE_KEY,
                            messageArguments = listOf(actor?.name.orEmpty(), post?.title.orEmpty()),
                        ),
                    thumbnailUrl = notificationPayloadService.resolveThumbnailUrl(media),
                    arguments = command.arguments,
                )
        }
    }

    private fun createMedia(
        post: Post?,
        postThumbnailUrlByPostId: Map<UUID, String>,
    ): NotificationPayloadService.NotificationPayloadMedia =
        NotificationPayloadService.NotificationPayloadMedia(
            url = post?.id?.let(postThumbnailUrlByPostId::get) ?: notificationPayloadResourceResolver.defaultPostThumbnailUrl(),
        )

    private companion object {
        const val MESSAGE_KEY = "notification.payload.follower-post"
    }
}
