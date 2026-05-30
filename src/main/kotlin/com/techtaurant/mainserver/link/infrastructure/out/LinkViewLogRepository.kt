package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkViewLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface LinkViewLogRepository : JpaRepository<LinkViewLog, UUID> {
    @Query(
        """
        SELECT DISTINCT lvl.link.id
        FROM LinkViewLog lvl
        WHERE lvl.user.id = :userId
        AND lvl.link.id IN :linkIds
        """,
    )
    fun findDistinctLinkIdsByUserIdAndLinkIdIn(
        @Param("userId") userId: UUID,
        @Param("linkIds") linkIds: List<UUID>,
    ): List<UUID>
}
