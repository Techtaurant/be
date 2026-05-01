package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LinkCrawlBatchRepository : JpaRepository<LinkCrawlBatch, UUID> {
    fun findAllByCompanyUserId(companyUserId: UUID): List<LinkCrawlBatch>

    fun findAllByActiveTrue(): List<LinkCrawlBatch>
}
