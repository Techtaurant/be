package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.UserLink
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserLinkRepository : JpaRepository<UserLink, UUID> {
    fun findByUserIdAndLinkId(
        userId: UUID,
        linkId: UUID,
    ): UserLink?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ul FROM UserLink ul WHERE ul.user.id = :userId AND ul.link.id = :linkId")
    fun findByUserIdAndLinkIdForUpdate(
        @Param("userId") userId: UUID,
        @Param("linkId") linkId: UUID,
    ): UserLink?

    fun findByUserIdAndLinkIdIn(
        userId: UUID,
        linkIds: List<UUID>,
    ): List<UserLink>
}
