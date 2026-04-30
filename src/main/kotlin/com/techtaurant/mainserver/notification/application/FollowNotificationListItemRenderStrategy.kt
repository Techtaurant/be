package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.dto.NotificationListItemResponse
import com.techtaurant.mainserver.notification.entity.NotificationArgument
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class FollowNotificationListItemRenderStrategy(
    private val notificationPayloadService: NotificationPayloadService,
    private val notificationPayloadResourceResolver: NotificationPayloadResourceResolver,
) : NotificationListItemRenderStrategy {
    override val type: NotificationType = NotificationType.FOLLOW

    override fun render(commands: List<NotificationListItemRenderCommand>): Map<UUID, NotificationListItemResponse> {
        if (commands.isEmpty()) {
            return emptyMap()
        }

        val normalizedArgumentsByNotificationId =
            commands.associate { command ->
                command.recipient.notification.id!! to
                    normalizeArguments(
                        arguments = command.arguments,
                        recipientUserId = command.recipient.recipientUser.id!!,
                    )
            }
        val actorUserIds =
            normalizedArgumentsByNotificationId.values
                .mapNotNull { it.findTargetId(NotificationTargetType.USER) }
        val actorsById = notificationPayloadResourceResolver.findUsersById(actorUserIds)
        val actorProfileImageUrlByUserId =
            notificationPayloadResourceResolver.resolveActorProfileImageUrlByUserId(actorsById)

        return commands.associate { command ->
            val recipient = command.recipient
            val notificationId = recipient.notification.id!!
            val arguments = normalizedArgumentsByNotificationId.getValue(notificationId)
            val actor = arguments.findTargetId(NotificationTargetType.USER)?.let(actorsById::get)

            notificationId to
                NotificationListItemResponse.from(
                    recipient = recipient,
                    payloadHtml =
                        notificationPayloadService.buildPayload(
                            messageKey = "notification.payload.follow",
                            messageArguments = listOf(actor?.name.orEmpty()),
                            media =
                                NotificationPayloadService.NotificationPayloadMedia(
                                    url =
                                        notificationPayloadResourceResolver.resolveActorProfileImageUrl(
                                            actor,
                                            actorProfileImageUrlByUserId,
                                        ),
                                    alt =
                                        actor?.name?.takeIf { it.isNotBlank() }?.let { "$it 프로필 이미지" }
                                            ?: "사용자 프로필 이미지",
                                ),
                        ),
                    arguments = arguments,
                )
        }
    }

    private fun normalizeArguments(
        arguments: List<NotificationArgument>,
        recipientUserId: UUID,
    ): List<NotificationArgument> =
        arguments.filterNot { it.targetType == NotificationTargetType.USER && it.targetId == recipientUserId }
}
