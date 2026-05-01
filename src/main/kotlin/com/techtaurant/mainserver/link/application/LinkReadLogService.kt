package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.entity.LinkReadLog
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LinkReadLogService(
    private val linkReadLogRepository: LinkReadLogRepository,
    private val linkRepository: LinkRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun toggleReadStatus(
        linkId: UUID,
        userId: UUID,
        isRead: Boolean,
    ) {
        val link =
            linkRepository.findById(linkId).orElseThrow {
                ApiException(LinkStatus.LINK_NOT_FOUND)
            }
        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.USER_NOT_FOUND)
            }
        val existingLog = linkReadLogRepository.findByUserIdAndLinkId(userId, linkId)

        if (isRead) {
            if (existingLog == null) {
                linkReadLogRepository.save(LinkReadLog(user = user, link = link))
            }
        } else {
            if (existingLog != null) {
                linkReadLogRepository.delete(existingLog)
            }
        }
    }
}
