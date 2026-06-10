package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface LinkCrawlBatchRepository : JpaRepository<LinkCrawlBatch, UUID> {
    fun findAllByCompanyUserId(companyUserId: UUID): List<LinkCrawlBatch>

    fun findAllByActiveTrue(): List<LinkCrawlBatch>

    @Modifying
    @Query(
        value = "DELETE FROM link_crawl_batches WHERE company_user_id = :companyUserId",
        nativeQuery = true,
    )
    fun deleteAllByCompanyUserId(
        @Param("companyUserId") companyUserId: UUID,
    ): Int
}
