package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.notification.enums.NotificationType
import org.springframework.stereotype.Component

@Component
class NotificationPayloadRenderer(
    strategies: List<NotificationPayloadRenderStrategy>,
) {
    private val strategyByType = createNotificationPayloadStrategyByType(strategies)

    fun render(
        type: NotificationType,
        commands: List<NotificationPayloadRenderCommand>,
    ): Map<java.util.UUID, NotificationPayloadRenderResult> {
        if (commands.isEmpty()) {
            return emptyMap()
        }

        return selectNotificationPayloadStrategy(type).render(commands)
    }

    private fun createNotificationPayloadStrategyByType(
        strategies: List<NotificationPayloadRenderStrategy>,
    ): Map<NotificationType, NotificationPayloadRenderStrategy> {
        val strategiesByType = strategies.groupBy { it.type }
        val duplicatedTypes = strategiesByType.filterValues { it.size > 1 }.keys
        require(duplicatedTypes.isEmpty()) {
            "알림 payload 렌더 전략이 중복 등록되었습니다: ${duplicatedTypes.joinToString()}"
        }

        val missingTypes = NotificationType.entries.filterNot { strategiesByType.containsKey(it) }
        require(missingTypes.isEmpty()) {
            "알림 payload 렌더 전략이 누락되었습니다: ${missingTypes.joinToString()}"
        }

        return strategiesByType.mapValues { (_, strategyGroup) -> strategyGroup.single() }
    }

    private fun selectNotificationPayloadStrategy(type: NotificationType): NotificationPayloadRenderStrategy =
        strategyByType.getValue(type)
}
