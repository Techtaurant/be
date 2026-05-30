package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.UserLink
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserLinkRepository : JpaRepository<UserLink, UUID> {
    fun findByUserIdAndLinkId(
        userId: UUID,
        linkId: UUID,
    ): UserLink?

    fun findByUserIdAndLinkIdIn(
        userId: UUID,
        linkIds: List<UUID>,
    ): List<UserLink>

    @Query(
        """
        SELECT userLink
        FROM UserLink userLink
        WHERE userLink.link.id = :linkId
        ORDER BY userLink.createdAt ASC, userLink.id ASC
        """,
    )
    fun findFirstSourceByLinkId(
        @Param("linkId") linkId: UUID,
        pageable: Pageable,
    ): List<UserLink>
}
