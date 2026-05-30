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

    @Modifying
    @Query(
        value = "DELETE FROM links WHERE source_company_user_id = :companyUserId",
        nativeQuery = true,
    )
    fun deleteAllBySourceCompanyUserId(
        @Param("companyUserId") companyUserId: UUID,
    ): Int
}
