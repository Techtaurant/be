package com.techtaurant.mainserver.notification.infrastructure.out

import com.techtaurant.mainserver.notification.entity.NotificationRecipient
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface NotificationRecipientRepository : JpaRepository<NotificationRecipient, UUID> {
    fun findAllByNotificationIdOrderByCreatedAtAsc(notificationId: UUID): List<NotificationRecipient>

    @EntityGraph(attributePaths = ["notification"])
    fun findAllByRecipientUserIdOrderByCreatedAtDescIdDesc(
        userId: UUID,
        pageable: Pageable,
    ): List<NotificationRecipient>

    @EntityGraph(attributePaths = ["notification"])
    @Query(
        """
        select nr
        from NotificationRecipient nr
        where nr.recipientUser.id = :userId
          and (nr.createdAt < :cursorCreatedAt or (nr.createdAt = :cursorCreatedAt and nr.id < :cursorId))
        order by nr.createdAt desc, nr.id desc
        """,
    )
    fun findPageByUserIdAndCursor(
        @Param("userId") userId: UUID,
        @Param("cursorCreatedAt") cursorCreatedAt: Instant,
        @Param("cursorId") cursorId: UUID,
        pageable: Pageable,
    ): List<NotificationRecipient>

    @EntityGraph(attributePaths = ["notification"])
    fun findAllByRecipientUserIdAndNotificationIdInAndReadAtIsNull(
        userId: UUID,
        notificationIds: Collection<UUID>,
    ): List<NotificationRecipient>

    fun countByRecipientUserIdAndReadAtIsNull(userId: UUID): Long
}
