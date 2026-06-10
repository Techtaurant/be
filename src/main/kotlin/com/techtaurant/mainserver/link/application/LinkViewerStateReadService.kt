package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.link.dto.LinkViewerStateResponse
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LinkViewerStateReadService(
    private val linkRepository: LinkRepository,
    private val userLinkRepository: UserLinkRepository,
    private val linkReadLogRepository: LinkReadLogRepository,
) {
    fun getLinkViewerStates(
        userId: UUID,
        linkIds: List<UUID>,
    ): List<LinkViewerStateResponse> {
        val normalizedLinkIds = linkIds.distinct()
        if (normalizedLinkIds.isEmpty()) {
            return emptyList()
        }

        val linkById = linkRepository.findAllById(normalizedLinkIds).associateBy { it.id!! }
        val loadedLinkIds = normalizedLinkIds.filter { linkById.containsKey(it) }
        if (loadedLinkIds.isEmpty()) {
            return emptyList()
        }

        val savedLinkIds =
            userLinkRepository
                .findSavedByUserIdAndLinkIdIn(userId = userId, linkIds = loadedLinkIds)
                .map { it.link.id!! }
                .toSet()
        val readLinkIds =
            linkReadLogRepository
                .findByUserIdAndLinkIdIn(userId = userId, linkIds = loadedLinkIds)
                .map { it.link.id!! }
                .toSet()

        return loadedLinkIds.map { linkId ->
            LinkViewerStateResponse(
                linkId = linkId,
                isSaved = linkId in savedLinkIds,
                isRead = linkId in readLinkIds,
            )
        }
    }
}
