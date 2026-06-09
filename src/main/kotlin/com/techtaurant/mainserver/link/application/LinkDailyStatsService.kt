package com.techtaurant.mainserver.link.application

import com.github.f4b6a3.uuid.UuidCreator
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class LinkDailyStatsService(
    private val linkDailyStatsRepository: LinkDailyStatsRepository,
) {
    fun incrementViewCount(
        linkId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::incrementViewCount)
    }

    fun incrementLikeCount(
        linkId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::incrementLikeCount)
    }

    fun decrementLikeCount(
        linkId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::decrementLikeCount)
    }

    fun incrementSaveCount(
        linkId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(linkId, statDate, linkDailyStatsRepository::incrementSaveCount)
    }

    fun decrementSaveCount(
        linkId: UUID,
        statDate: LocalDate,
    ) {
        linkDailyStatsRepository.decrementSaveCount(linkId, statDate)
    }

    private fun applyDailyStatsChange(
        linkId: UUID,
        statDate: LocalDate,
        changeFn: (UUID, LocalDate) -> Int,
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
