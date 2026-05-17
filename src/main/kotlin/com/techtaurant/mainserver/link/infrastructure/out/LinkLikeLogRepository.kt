package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkLikeLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LinkLikeLogRepository : JpaRepository<LinkLikeLog, UUID> {
    fun findByLinkIdAndUserId(
        linkId: UUID,
        userId: UUID,
    ): LinkLikeLog?

    fun findByUserIdAndLinkIdIn(
        userId: UUID,
        linkIds: List<UUID>,
    ): List<LinkLikeLog>
}
