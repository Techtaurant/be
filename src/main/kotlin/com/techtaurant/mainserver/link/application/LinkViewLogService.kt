package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.entity.LinkViewLog
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkViewLogRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LinkViewLogService(
    private val linkViewLogRepository: LinkViewLogRepository,
    private val linkRepository: LinkRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun recordView(
        linkId: UUID,
        userId: UUID?,
        ipAddress: String?,
        userAgent: String?,
    ) {
        val link =
            linkRepository.findById(linkId).orElseThrow {
                ApiException(LinkStatus.LINK_NOT_FOUND)
            }

        val user =
            userId?.let {
                userRepository.findById(it).orElse(null)
            }

        linkViewLogRepository.save(
            LinkViewLog(
                link = link,
                user = user,
                ipAddress = ipAddress,
                userAgent = userAgent,
            ),
        )

        linkRepository.incrementViewCount(linkId)
    }
}
