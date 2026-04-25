package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.user.entity.User
import java.util.UUID

internal abstract class AbstractActorPostNotificationPayloadStrategy(
    private val notificationPayloadService: NotificationPayloadService,
    private val notificationPayloadResourceResolver: NotificationPayloadResourceResolver,
) : NotificationPayloadRenderStrategy {
    protected abstract val messageKey: String

    final override fun render(commands: List<NotificationPayloadRenderCommand>): Map<UUID, NotificationPayloadRenderResult> {
        if (commands.isEmpty()) {
            return emptyMap()
        }

        val actorUserIds = commands.mapNotNull { it.arguments.findTargetId(NotificationTargetType.USER) }
        val actorsById = notificationPayloadResourceResolver.findUsersById(actorUserIds)
        val actorProfileImageUrlByUserId =
            notificationPayloadResourceResolver.resolveActorProfileImageUrlByUserId(actorsById)

        val postIds = commands.mapNotNull { it.arguments.findTargetId(NotificationTargetType.POST) }
        val postsById = notificationPayloadResourceResolver.findPostsById(postIds)
        val postThumbnailUrlByPostId =
            notificationPayloadResourceResolver.resolvePostThumbnailUrlByPostId(postsById)

        return commands.associate { command ->
            val actor = command.arguments.findTargetId(NotificationTargetType.USER)?.let(actorsById::get)
            val post = command.arguments.findTargetId(NotificationTargetType.POST)?.let(postsById::get)

            command.notificationId to
                NotificationPayloadRenderResult(
                    payloadHtml =
                        notificationPayloadService.buildPayload(
                            messageKey = messageKey,
                            messageArguments = listOf(actor?.name.orEmpty(), post?.title.orEmpty()),
                            media = createMedia(actor, post, actorProfileImageUrlByUserId, postThumbnailUrlByPostId),
                        ),
                    arguments = command.arguments,
                )
        }
    }

    protected abstract fun createMedia(
        actor: User?,
        post: Post?,
        actorProfileImageUrlByUserId: Map<UUID, String>,
        postThumbnailUrlByPostId: Map<UUID, String>,
    ): NotificationPayloadService.NotificationPayloadMedia

    protected fun createActorProfileMedia(
        actor: User?,
        actorProfileImageUrlByUserId: Map<UUID, String>,
    ): NotificationPayloadService.NotificationPayloadMedia =
        NotificationPayloadService.NotificationPayloadMedia(
            url = notificationPayloadResourceResolver.resolveActorProfileImageUrl(actor, actorProfileImageUrlByUserId),
            alt = actor?.name?.takeIf { it.isNotBlank() }?.let { "$it 프로필 이미지" } ?: "사용자 프로필 이미지",
        )

    protected fun createPostThumbnailMedia(
        post: Post?,
        postThumbnailUrlByPostId: Map<UUID, String>,
    ): NotificationPayloadService.NotificationPayloadMedia =
        NotificationPayloadService.NotificationPayloadMedia(
            url = post?.id?.let(postThumbnailUrlByPostId::get) ?: notificationPayloadResourceResolver.defaultPostThumbnailUrl(),
            alt = post?.title?.let { "$it 썸네일" } ?: "게시물 썸네일",
        )
}
