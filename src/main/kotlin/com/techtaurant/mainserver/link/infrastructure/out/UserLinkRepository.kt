package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.UserLink
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface UserLinkRepository : JpaRepository<UserLink, UUID> {
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        INSERT INTO user_links (id, user_id, link_id, is_source, created_at_utc, updated_at_utc)
        VALUES (:id, :userId, :linkId, :isSource, :createdAt, :updatedAt)
        ON CONFLICT ON CONSTRAINT uk_user_links_user_id_link_id_is_source DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("userId") userId: UUID,
        @Param("linkId") linkId: UUID,
        @Param("isSource") isSource: Boolean,
        @Param("createdAt") createdAt: Instant,
        @Param("updatedAt") updatedAt: Instant,
    ): Int

    @Query(
        """
        SELECT userLink
        FROM UserLink userLink
        WHERE userLink.user.id = :userId
          AND userLink.link.id = :linkId
          AND userLink.isSource = true
        """,
    )
    fun findSourceByUserIdAndLinkId(
        @Param("userId") userId: UUID,
        @Param("linkId") linkId: UUID,
    ): UserLink?

    @Query(
        """
        SELECT userLink
        FROM UserLink userLink
        WHERE userLink.user.id = :userId
          AND userLink.link.id = :linkId
          AND userLink.isSource = false
        """,
    )
    fun findSavedByUserIdAndLinkId(
        @Param("userId") userId: UUID,
        @Param("linkId") linkId: UUID,
    ): UserLink?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT userLink
        FROM UserLink userLink
        WHERE userLink.user.id = :userId
          AND userLink.link.id = :linkId
          AND userLink.isSource = false
        """,
    )
    fun findSavedByUserIdAndLinkIdForUpdate(
        @Param("userId") userId: UUID,
        @Param("linkId") linkId: UUID,
    ): UserLink?

    @Query(
        """
        SELECT userLink
        FROM UserLink userLink
        WHERE userLink.user.id = :userId
          AND userLink.link.id IN :linkIds
          AND userLink.isSource = false
        """,
    )
    fun findSavedByUserIdAndLinkIdIn(
        @Param("userId") userId: UUID,
        @Param("linkIds") linkIds: List<UUID>,
    ): List<UserLink>

    @Query(
        """
        SELECT userLink
        FROM UserLink userLink
        JOIN FETCH userLink.link
        WHERE userLink.user.id = :userId
          AND userLink.isSource = false
        """,
    )
    fun findSavedByUserId(
        @Param("userId") userId: UUID,
    ): List<UserLink>

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        DELETE FROM UserLink userLink
        WHERE userLink.user.id = :userId
          AND userLink.isSource = true
        """,
    )
    fun deleteAllSourcesByUserId(
        @Param("userId") userId: UUID,
    ): Int

    @Query(
        """
        SELECT userLink
        FROM UserLink userLink
        WHERE userLink.link.id = :linkId
          AND userLink.isSource = true
          AND userLink.user.role = 'COMPANY'
        ORDER BY userLink.createdAt ASC, userLink.id ASC
        """,
    )
    fun findFirstSourceByLinkId(
        @Param("linkId") linkId: UUID,
        pageable: Pageable,
    ): List<UserLink>
}
