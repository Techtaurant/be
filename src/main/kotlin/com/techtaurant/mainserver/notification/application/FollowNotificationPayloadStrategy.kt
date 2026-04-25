package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.entity.NotificationArgument
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class FollowNotificationPayloadStrategy(
    private val notificationPayloadService: NotificationPayloadService,
    private val notificationPayloadResourceResolver: NotificationPayloadResourceResolver,
) : NotificationPayloadRenderStrategy {
    override val type: NotificationType = NotificationType.FOLLOW

    override fun render(commands: List<NotificationPayloadRenderCommand>): Map<UUID, NotificationPayloadRenderResult> {
        if (commands.isEmpty()) {
            return emptyMap()
        }

        val normalizedArgumentsByNotificationId =
            commands.associate { command ->
                command.notificationId to normalizeArguments(command.arguments, command.recipientUserId)
            }
        val actorUserIds =
            normalizedArgumentsByNotificationId.values
                .mapNotNull { it.findTargetId(NotificationTargetType.USER) }
        val actorsById = notificationPayloadResourceResolver.findUsersById(actorUserIds)
        val actorProfileImageUrlByUserId =
            notificationPayloadResourceResolver.resolveActorProfileImageUrlByUserId(actorsById)

        return commands.associate { command ->
            val arguments = normalizedArgumentsByNotificationId.getValue(command.notificationId)
            val actor = arguments.findTargetId(NotificationTargetType.USER)?.let(actorsById::get)

            command.notificationId to
                NotificationPayloadRenderResult(
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
