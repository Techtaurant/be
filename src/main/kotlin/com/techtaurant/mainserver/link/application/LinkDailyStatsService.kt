package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.sql.Date
import java.util.UUID

@Service
class LinkDailyStatsService(
    private val linkDailyStatsRepository: LinkDailyStatsRepository,
    private val linkRepository: LinkRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun incrementViewCount(
        linkId: UUID,
        statDate: Date,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::incrementViewCount)
    }

    fun incrementLikeCount(
        linkId: UUID,
        statDate: Date,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::incrementLikeCount)
    }

    fun decrementLikeCount(
        linkId: UUID,
        statDate: Date,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::decrementLikeCount)
    }

    fun incrementSaveCount(
        linkId: UUID,
        statDate: Date,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::incrementSaveCount)
    }

    fun decrementSaveCount(
        linkId: UUID,
        statDate: Date,
    ) {
        linkDailyStatsRepository.decrementSaveCount(linkId, statDate)
    }

    private fun applyDailyStatsChange(
        linkId: UUID,
        statDate: Date,
        changeFn: (UUID, Date) -> Int,
    ) {
        if (changeFn(linkId, statDate) == 0) {
            retryDailyStatsChangeAfterCreate(linkId, statDate, changeFn)
        }
    }

    private fun retryDailyStatsChangeAfterCreate(
        linkId: UUID,
        statDate: Date,
        changeFn: (UUID, Date) -> Int,
    ) {
        try {
            createDailyStats(linkId, statDate)
        } catch (e: DataIntegrityViolationException) {
            logger.debug("LinkDailyStats 동시 생성 감지, 변경 쿼리 재시도: linkId={}", linkId)
        }
        changeFn(linkId, statDate)
    }

    private fun createDailyStats(
        linkId: UUID,
        statDate: Date,
    ) {
        val link = linkRepository.getReferenceById(linkId)
        val dailyStats = LinkDailyStats(link = link, statDate = statDate)
        linkDailyStatsRepository.saveAndFlush(dailyStats)
    }
}
