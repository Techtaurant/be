package com.techtaurant.mainserver.link.application

import com.github.f4b6a3.uuid.UuidCreator
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LinkSaveService(
    private val userLinkRepository: UserLinkRepository,
    private val linkRepository: LinkRepository,
    private val userRepository: UserRepository,
    private val linkDailyStatsService: LinkDailyStatsService,
) {
    @Transactional
    fun save(
        linkId: UUID,
        userId: UUID,
    ) {
        linkRepository.findById(linkId).orElseThrow {
            ApiException(LinkStatus.LINK_NOT_FOUND)
        }
        userRepository.findById(userId).orElseThrow {
            ApiException(UserStatus.USER_NOT_FOUND)
        }

        val inserted =
            userLinkRepository.insertIfAbsent(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = userId,
                linkId = linkId,
            )
        if (inserted == 1) {
            linkDailyStatsService.incrementSaveCount(linkId, DateUtils.today())
        }
    }

    @Transactional
    fun unsave(
        linkId: UUID,
        userId: UUID,
    ) {
        val existingRelation = userLinkRepository.findByUserIdAndLinkIdForUpdate(userId, linkId)
        if (existingRelation != null) {
            val statDate = DateUtils.toUtcDate(existingRelation.createdAt)
            userLinkRepository.delete(existingRelation)
            linkDailyStatsService.decrementSaveCount(linkId, statDate)
        }
    }
}
