package com.techtaurant.mainserver.link.application

import com.github.f4b6a3.uuid.UuidCreator
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.entity.LinkLikeLog
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkLikeLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LinkLikeLogService(
    private val linkLikeLogRepository: LinkLikeLogRepository,
    private val linkRepository: LinkRepository,
    private val userRepository: UserRepository,
    private val linkDailyStatsService: LinkDailyStatsService,
) {
    @Transactional
    fun recordLike(
        linkId: UUID,
        userId: UUID,
        likeStatus: LikeStatus,
    ) {
        linkRepository.findById(linkId).orElseThrow {
            ApiException(LinkStatus.LINK_NOT_FOUND)
        }

        userRepository.findById(userId).orElseThrow {
            ApiException(UserStatus.ID_NOT_FOUND)
        }

        val existingLog = linkLikeLogRepository.findByLinkIdAndUserIdForUpdate(linkId, userId)
        val eventStatDate = DateUtils.today()

        if (existingLog != null) {
            applyExistingLogChange(existingLog, linkId, likeStatus, eventStatDate)
        } else {
            when (likeStatus) {
                LikeStatus.NONE -> { }
                LikeStatus.LIKE -> insertLogIfAbsent(linkId, userId, likeStatus, true, eventStatDate)
                LikeStatus.DISLIKE -> insertLogIfAbsent(linkId, userId, likeStatus, false, eventStatDate)
            }
        }
    }

    private fun applyExistingLogChange(
        existingLog: LinkLikeLog,
        linkId: UUID,
        likeStatus: LikeStatus,
        eventStatDate: java.sql.Date,
    ) {
        val previousIsLiked = existingLog.isLiked

        when (likeStatus) {
            LikeStatus.NONE -> {
                linkLikeLogRepository.delete(existingLog)
                updateLikeCount(linkId, !previousIsLiked, eventStatDate)
            }
            LikeStatus.LIKE -> {
                if (!previousIsLiked) {
                    existingLog.isLiked = true
                    linkLikeLogRepository.save(existingLog)
                    updateLikeCount(linkId, true, eventStatDate)
                    updateLikeCount(linkId, true, eventStatDate)
                }
            }
            LikeStatus.DISLIKE -> {
                if (previousIsLiked) {
                    existingLog.isLiked = false
                    linkLikeLogRepository.save(existingLog)
                    updateLikeCount(linkId, false, eventStatDate)
                    updateLikeCount(linkId, false, eventStatDate)
                }
            }
        }
    }

    private fun insertLogIfAbsent(
        linkId: UUID,
        userId: UUID,
        likeStatus: LikeStatus,
        isLiked: Boolean,
        eventStatDate: java.sql.Date,
    ) {
        val inserted =
            linkLikeLogRepository.insertIfAbsent(
                id = UuidCreator.getTimeOrderedEpoch(),
                linkId = linkId,
                userId = userId,
                isLiked = isLiked,
            )

        if (inserted == 1) {
            updateLikeCount(linkId, isLiked, eventStatDate)
        } else {
            val existingLog = linkLikeLogRepository.findByLinkIdAndUserIdForUpdate(linkId, userId)
            if (existingLog != null) {
                applyExistingLogChange(existingLog, linkId, likeStatus, eventStatDate)
            }
        }
    }

    private fun updateLikeCount(
        linkId: UUID,
        increment: Boolean,
        statDate: java.sql.Date,
    ) {
        if (increment) {
            linkRepository.incrementLikeCount(linkId)
            linkDailyStatsService.incrementLikeCount(linkId, statDate)
        } else {
            linkRepository.decrementLikeCount(linkId)
            linkDailyStatsService.decrementLikeCount(linkId, statDate)
        }
    }
}
