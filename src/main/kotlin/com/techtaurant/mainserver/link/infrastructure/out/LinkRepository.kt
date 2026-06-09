package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.Link
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface LinkRepository : JpaRepository<Link, UUID> {
    fun findByUrl(url: String): Link?

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
              AND userLink.isSource = true
        )
        """,
    )
    fun findAllByConnectedUserIdWithTags(
        @Param("companyUserId") companyUserId: UUID,
    ): List<Link>

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        value = """
        DELETE FROM links deleted_link
        WHERE EXISTS (
            SELECT 1
            FROM user_links company_user_link
            WHERE company_user_link.link_id = deleted_link.id
              AND company_user_link.user_id = :companyUserId
              AND company_user_link.is_source = TRUE
        )
          AND NOT EXISTS (
            SELECT 1
            FROM user_links other_company_user_link
            JOIN users other_company_user ON other_company_user.id = other_company_user_link.user_id
            WHERE other_company_user_link.link_id = deleted_link.id
              AND other_company_user_link.user_id <> :companyUserId
              AND other_company_user_link.is_source = TRUE
              AND other_company_user.role = 'COMPANY'
        )
        """,
        nativeQuery = true,
    )
    fun deleteAllOnlyConnectedByCompanyUserId(
        @Param("companyUserId") companyUserId: UUID,
    ): Int

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
        WHERE EXISTS (
            SELECT validSourceUserLink.id
            FROM UserLink validSourceUserLink
            WHERE validSourceUserLink.link = l
              AND validSourceUserLink.isSource = true
              AND validSourceUserLink.user.role = 'COMPANY'
        )
          AND (
            :sourceCompanyUserId IS NULL OR EXISTS (
                SELECT sourceUserLink.id
                FROM UserLink sourceUserLink
                WHERE sourceUserLink.link = l
                  AND sourceUserLink.user.id = :sourceCompanyUserId
                  AND sourceUserLink.isSource = true
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
        WHERE EXISTS (
            SELECT validSourceUserLink.id
            FROM UserLink validSourceUserLink
            WHERE validSourceUserLink.link = l
              AND validSourceUserLink.isSource = true
              AND validSourceUserLink.user.role = 'COMPANY'
        )
          AND (
            :sourceCompanyUserId IS NULL OR EXISTS (
                SELECT sourceUserLink.id
                FROM UserLink sourceUserLink
                WHERE sourceUserLink.link = l
                  AND sourceUserLink.user.id = :sourceCompanyUserId
                  AND sourceUserLink.isSource = true
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
        @Param("cursorCreatedAt") cursorCreatedAt: Instant,
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
