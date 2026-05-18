package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.entity.UserLink
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
        val link =
            linkRepository.findById(linkId).orElseThrow {
                ApiException(LinkStatus.LINK_NOT_FOUND)
            }
        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.USER_NOT_FOUND)
            }

        if (userLinkRepository.findByUserIdAndLinkId(userId, linkId) == null) {
            val savedUserLink =
                userLinkRepository.save(
                    UserLink(
                        user = user,
                        link = link,
                    ),
                )
            linkDailyStatsService.incrementSaveCount(linkId, DateUtils.toUtcDate(savedUserLink.createdAt))
        }
    }

    @Transactional
    fun unsave(
        linkId: UUID,
        userId: UUID,
    ) {
        val existingRelation = userLinkRepository.findByUserIdAndLinkId(userId, linkId)
        if (existingRelation != null) {
            val statDate = DateUtils.toUtcDate(existingRelation.createdAt)
            userLinkRepository.delete(existingRelation)
            linkDailyStatsService.decrementSaveCount(linkId, statDate)
        }
    }
}
