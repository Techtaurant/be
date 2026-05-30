package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkLikeLog
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface LinkLikeLogRepository : JpaRepository<LinkLikeLog, UUID> {
    fun findByLinkIdAndUserId(
        linkId: UUID,
        userId: UUID,
    ): LinkLikeLog?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LinkLikeLog l WHERE l.link.id = :linkId AND l.user.id = :userId")
    fun findByLinkIdAndUserIdForUpdate(
        @Param("linkId") linkId: UUID,
        @Param("userId") userId: UUID,
    ): LinkLikeLog?

    fun findByUserIdAndLinkIdIn(
        userId: UUID,
        linkIds: List<UUID>,
    ): List<LinkLikeLog>
}
