package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.link.dto.LinkStatsResponse
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LinkStatsReadService(
    private val linkDailyStatsRepository: LinkDailyStatsRepository,
) {
    fun getLinkStats(linkIds: List<UUID>): List<LinkStatsResponse> {
        val normalizedLinkIds = linkIds.distinct()
        if (normalizedLinkIds.isEmpty()) {
            return emptyList()
        }

        val statsByLinkId =
            linkDailyStatsRepository.aggregateStatsByLinkIds(normalizedLinkIds).associateBy { it.getLinkId() }

        return normalizedLinkIds.mapNotNull { linkId ->
            statsByLinkId[linkId]?.let { stats ->
                LinkStatsResponse(
                    linkId = stats.getLinkId(),
                    viewCount = stats.getViewCount(),
                    likeCount = stats.getLikeCount(),
                    saveCount = stats.getSaveCount(),
                )
            }
        }
    }
}
