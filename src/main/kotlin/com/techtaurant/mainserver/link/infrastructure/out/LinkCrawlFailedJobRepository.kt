package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkCrawlFailedJob
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LinkCrawlFailedJobRepository : JpaRepository<LinkCrawlFailedJob, UUID> {
    fun findAllByBatchIdOrderByCreatedAtAsc(batchId: UUID): List<LinkCrawlFailedJob>

    fun findByBatchIdAndArticleUrl(
        batchId: UUID,
        articleUrl: String,
    ): LinkCrawlFailedJob?
}
