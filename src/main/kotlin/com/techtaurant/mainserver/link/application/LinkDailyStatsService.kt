package com.techtaurant.mainserver.link.application

import com.github.f4b6a3.uuid.UuidCreator
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import org.springframework.stereotype.Service
import java.sql.Date
import java.util.UUID

@Service
class LinkDailyStatsService(
    private val linkDailyStatsRepository: LinkDailyStatsRepository,
) {
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
            linkDailyStatsRepository.insertIfAbsent(
                id = UuidCreator.getTimeOrderedEpoch(),
                linkId = linkId,
                statDate = statDate,
            )
            changeFn(linkId, statDate)
        }
    }
}
