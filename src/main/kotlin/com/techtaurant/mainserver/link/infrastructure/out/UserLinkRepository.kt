package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.UserLink
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
        SELECT DISTINCT userLink
        FROM UserLink userLink
        JOIN FETCH userLink.user
        JOIN FETCH userLink.link
        WHERE userLink.link.id IN :linkIds
        """,
    )
    fun findAllByLinkIdInWithUserAndLink(
        @Param("linkIds") linkIds: List<UUID>,
    ): List<UserLink>
}
