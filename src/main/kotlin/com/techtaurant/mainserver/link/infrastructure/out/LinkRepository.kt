package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.Link
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface LinkRepository : JpaRepository<Link, UUID> {
    fun findByUrl(url: String): Link?

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
        WHERE l.sourceCompanyUser.id = :companyUserId
        """,
    )
    fun findAllBySourceCompanyUserIdWithTags(
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
}
