package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.common.exception.ApiException
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
) {
    @Transactional
    fun recordLike(
        linkId: UUID,
        userId: UUID,
        likeStatus: LikeStatus,
    ) {
        val link =
            linkRepository.findById(linkId).orElseThrow {
                ApiException(LinkStatus.LINK_NOT_FOUND)
            }

        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.ID_NOT_FOUND)
            }

        val existingLog = linkLikeLogRepository.findByLinkIdAndUserId(linkId, userId)

        if (existingLog != null) {
            val previousIsLiked = existingLog.isLiked

            when (likeStatus) {
                LikeStatus.NONE -> {
                    linkLikeLogRepository.delete(existingLog)
                    updateLikeCount(linkId, !previousIsLiked)
                }
                LikeStatus.LIKE -> {
                    if (!previousIsLiked) {
                        existingLog.isLiked = true
                        linkLikeLogRepository.save(existingLog)
                        updateLikeCount(linkId, true)
                        updateLikeCount(linkId, true)
                    }
                }
                LikeStatus.DISLIKE -> {
                    if (previousIsLiked) {
                        existingLog.isLiked = false
                        linkLikeLogRepository.save(existingLog)
                        updateLikeCount(linkId, false)
                        updateLikeCount(linkId, false)
                    }
                }
            }
        } else {
            when (likeStatus) {
                LikeStatus.NONE -> { }
                LikeStatus.LIKE -> {
                    linkLikeLogRepository.save(LinkLikeLog(link = link, user = user, isLiked = true))
                    updateLikeCount(linkId, true)
                }
                LikeStatus.DISLIKE -> {
                    linkLikeLogRepository.save(LinkLikeLog(link = link, user = user, isLiked = false))
                    updateLikeCount(linkId, false)
                }
            }
        }
    }

    private fun updateLikeCount(
        linkId: UUID,
        increment: Boolean,
    ) {
        if (increment) {
            linkRepository.incrementLikeCount(linkId)
        } else {
            linkRepository.decrementLikeCount(linkId)
        }
    }
}
