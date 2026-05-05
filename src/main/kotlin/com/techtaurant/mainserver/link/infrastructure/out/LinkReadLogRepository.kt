package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkReadLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LinkReadLogRepository : JpaRepository<LinkReadLog, UUID> {
    fun findByUserIdAndLinkId(
        userId: UUID,
        linkId: UUID,
    ): LinkReadLog?

    fun existsByUserIdAndLinkId(
        userId: UUID,
        linkId: UUID,
    ): Boolean

    fun findByUserIdAndLinkIdIn(
        userId: UUID,
        linkIds: List<UUID>,
    ): List<LinkReadLog>
}
