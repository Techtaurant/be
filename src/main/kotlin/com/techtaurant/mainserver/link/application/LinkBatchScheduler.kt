package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Component
class LinkBatchScheduler(
    private val linkCrawlBatchRepository: LinkCrawlBatchRepository,
    private val linkBatchRunService: LinkBatchRunService,
    private val clock: Clock,
) {
    @Scheduled(fixedDelay = 60_000L)
    fun runDueBatches() {
        val now = clock.instant()

        linkCrawlBatchRepository.findAllByActiveTrue()
            .filter { shouldRun(it, now) }
            .forEach { batch ->
                batch.id?.let(linkBatchRunService::run)
            }
    }

    internal fun shouldRun(
        batch: LinkCrawlBatch,
        now: Instant = clock.instant(),
    ): Boolean {
        val cronExpression = runCatching { CronExpression.parse(batch.cronExpression) }.getOrNull() ?: return false
        val nowUtc = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
        val referenceTime =
            batch.lastTriggeredAt?.let { ZonedDateTime.ofInstant(it, ZoneOffset.UTC) }
                ?: nowUtc.minusMinutes(1)

        val nextExecution = cronExpression.next(referenceTime) ?: return false
        return !nextExecution.isAfter(nowUtc)
    }
}
