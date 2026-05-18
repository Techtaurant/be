package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.UserLink
import org.springframework.data.jpa.repository.JpaRepository
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

    fun findFirstByLink_IdOrderByCreatedAtAscIdAsc(linkId: UUID): UserLink?
}
