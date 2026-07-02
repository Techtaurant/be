package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkCrawlFailedJob
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface LinkCrawlFailedJobRepository : JpaRepository<LinkCrawlFailedJob, UUID> {
    fun findAllByRunIdAndResolvedFalseOrderByCreatedAtAsc(runId: UUID): List<LinkCrawlFailedJob>

    fun findAllByResolvedFalseOrderByCreatedAtAsc(): List<LinkCrawlFailedJob>

    fun existsByRunIdAndResolvedFalse(runId: UUID): Boolean

    fun findByRunIdAndArticleUrl(
        runId: UUID,
        articleUrl: String,
    ): LinkCrawlFailedJob?

    @Query(
        "SELECT DISTINCT j.run.id FROM LinkCrawlFailedJob j WHERE j.run.id IN :runIds AND j.resolved = false",
    )
    fun findRunIdsWithUnresolvedJobs(runIds: Collection<UUID>): Set<UUID>
}
