package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.UserLink
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Date
import java.util.UUID

interface UserLinkRepository : JpaRepository<UserLink, UUID> {
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        INSERT INTO user_links (id, user_id, link_id, created_at, updated_at)
        VALUES (:id, :userId, :linkId, :createdAt, :updatedAt)
        ON CONFLICT ON CONSTRAINT uk_user_links_user_id_link_id DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("userId") userId: UUID,
        @Param("linkId") linkId: UUID,
        @Param("createdAt") createdAt: Date,
        @Param("updatedAt") updatedAt: Date,
    ): Int

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
