package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.user.entity.User
import java.util.UUID

/**
 * USER(actor)와 POST 인자를 함께 가진 알림 목록 아이템의 공통 렌더링 흐름입니다.
 *
 * 댓글, 대댓글, 팔로워 새 게시물처럼 payload 문구가 actor 이름과 post 제목으로 구성되는 알림은
 * 여기서 사용자, 게시물, 썸네일 리소스를 배치 조회합니다. 하위 전략은 알림 타입별 messageKey와
 * 목록 썸네일로 actor 프로필 또는 post 썸네일 중 무엇을 노출할지만 결정합니다.
 *
 * Follow 알림은 수신자 자신을 arguments에서 제외하는 별도 정규화가 필요해서 이 기반 전략을 사용하지 않습니다.
 */
internal abstract class AbstractActorPostNotificationListItemRenderStrategy(
    private val notificationPayloadService: NotificationPayloadService,
    private val notificationPayloadResourceResolver: NotificationPayloadResourceResolver,
) : NotificationListItemRenderStrategy {
    protected abstract val messageKey: String

    final override fun render(commands: List<NotificationListItemRenderCommand>): Map<UUID, NotificationListItemResponse> {
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
            val recipient = command.recipient
            val notificationId = recipient.notification.id!!
            val actor = command.arguments.findTargetId(NotificationTargetType.USER)?.let(actorsById::get)
            val post = command.arguments.findTargetId(NotificationTargetType.POST)?.let(postsById::get)
            val thumbnailMedia = selectThumbnailMedia(actor, post, actorProfileImageUrlByUserId, postThumbnailUrlByPostId)

            notificationId to
                NotificationListItemResponse.from(
                    recipient = recipient,
                    payloadHtml =
                        notificationPayloadService.buildPayload(
                            messageKey = messageKey,
                            messageArguments = listOf(actor?.name.orEmpty(), post?.title.orEmpty()),
                        ),
                    thumbnailUrl = notificationPayloadService.resolveThumbnailUrl(thumbnailMedia),
                    arguments = command.arguments,
                )
        }
    }

    protected abstract fun selectThumbnailMedia(
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
        )

    protected fun createPostThumbnailMedia(
        post: Post?,
        postThumbnailUrlByPostId: Map<UUID, String>,
    ): NotificationPayloadService.NotificationPayloadMedia =
        NotificationPayloadService.NotificationPayloadMedia(
            url = post?.id?.let(postThumbnailUrlByPostId::get) ?: notificationPayloadResourceResolver.defaultPostThumbnailUrl(),
        )
}
