package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.Link
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Date
import java.util.UUID

interface LinkRepository : JpaRepository<Link, UUID> {
    fun findByUrl(url: String): Link?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Link l WHERE l.id = :linkId")
    fun findByIdForUpdate(
        @Param("linkId") linkId: UUID,
    ): Link?

    @Query(
        """
        SELECT DISTINCT l
        FROM Link l
        LEFT JOIN FETCH l.tags
        WHERE l.id = :linkId
        """,
    )
    fun findByIdWithTags(
        @Param("linkId") linkId: UUID,
    ): Link?

    @Query(
        """
        SELECT DISTINCT l
        FROM Link l
        LEFT JOIN FETCH l.tags
        """,
    )
    fun findAllWithTags(): List<Link>

    @Query(
        """
        SELECT DISTINCT l
        FROM Link l
        LEFT JOIN FETCH l.tags
        WHERE EXISTS (
            SELECT userLink.id
            FROM UserLink userLink
            WHERE userLink.link = l
              AND userLink.user.id = :companyUserId
        )
        """,
    )
    fun findAllByConnectedUserIdWithTags(
        @Param("companyUserId") companyUserId: UUID,
    ): List<Link>

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Link l SET l.viewCount = l.viewCount + 1 WHERE l.id = :linkId")
    fun incrementViewCount(
        @Param("linkId") linkId: UUID,
    )

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Link l SET l.likeCount = l.likeCount + 1 WHERE l.id = :linkId")
    fun incrementLikeCount(
        @Param("linkId") linkId: UUID,
    )

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("UPDATE Link l SET l.likeCount = l.likeCount - 1 WHERE l.id = :linkId")
    fun decrementLikeCount(
        @Param("linkId") linkId: UUID,
    )

    @Query(
        """
        SELECT l.id
        FROM Link l
        WHERE (
            :sourceCompanyUserId IS NULL OR EXISTS (
                SELECT sourceUserLink.id
                FROM UserLink sourceUserLink
                WHERE sourceUserLink.link = l
                  AND sourceUserLink.user.id = :sourceCompanyUserId
            )
        )
          AND (
            :tag IS NULL OR EXISTS (
                SELECT taggedLink.id
                FROM Link taggedLink
                JOIN taggedLink.tags matchedTag
                WHERE taggedLink = l
                  AND matchedTag.name = :tag
            )
          )
        ORDER BY l.createdAt DESC, l.id DESC
        """,
    )
    fun findFirstPageIds(
        @Param("sourceCompanyUserId") sourceCompanyUserId: UUID?,
        @Param("tag") tag: String?,
        pageable: Pageable,
    ): List<UUID>

    @Query(
        """
        SELECT l.id
        FROM Link l
        WHERE (
            :sourceCompanyUserId IS NULL OR EXISTS (
                SELECT sourceUserLink.id
                FROM UserLink sourceUserLink
                WHERE sourceUserLink.link = l
                  AND sourceUserLink.user.id = :sourceCompanyUserId
            )
        )
          AND (
            :tag IS NULL OR EXISTS (
                SELECT taggedLink.id
                FROM Link taggedLink
                JOIN taggedLink.tags matchedTag
                WHERE taggedLink = l
                  AND matchedTag.name = :tag
            )
          )
          AND (
            l.createdAt < :cursorCreatedAt OR
            (l.createdAt = :cursorCreatedAt AND l.id < :cursorId)
          )
        ORDER BY l.createdAt DESC, l.id DESC
        """,
    )
    fun findNextPageIds(
        @Param("sourceCompanyUserId") sourceCompanyUserId: UUID?,
        @Param("tag") tag: String?,
        @Param("cursorCreatedAt") cursorCreatedAt: Date,
        @Param("cursorId") cursorId: UUID,
        pageable: Pageable,
    ): List<UUID>

    @Query(
        """
        SELECT DISTINCT l
        FROM Link l
        LEFT JOIN FETCH l.tags
        WHERE l.id IN :linkIds
        """,
    )
    fun findAllByIdInWithTags(
        @Param("linkIds") linkIds: List<UUID>,
    ): List<Link>
}
