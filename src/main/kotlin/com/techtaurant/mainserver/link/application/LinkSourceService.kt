package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 링크의 첫 번째 등록자(source)를 조회/검증하는 공통 서비스.
 *
 * 저장 순서상 가장 먼저 링크를 등록한 사용자를 source로 간주한다.
 */
@Service
class LinkSourceService(
    private val userLinkRepository: UserLinkRepository,
) {
    /**
     * 링크의 첫 번째 등록자 ID를 반환한다. 등록자가 없으면 null.
     */
    fun findSourceUserId(linkId: UUID): UUID? =
        userLinkRepository
            .findFirstSourceByLinkId(linkId, PageRequest.of(0, 1))
            .firstOrNull()
            ?.user
            ?.id

    /**
     * 링크의 첫 번째 등록자 ID를 반환한다. 등록자가 없으면 LINK_NOT_FOUND.
     */
    fun getSourceUserId(linkId: UUID): UUID = findSourceUserId(linkId) ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)

    /**
     * 주어진 사용자가 링크의 첫 번째 등록자(source)인지 검증한다.
     * source가 아니면 CANNOT_MODIFY_LINK.
     */
    fun validateSource(
        linkId: UUID,
        userId: UUID,
    ) {
        if (getSourceUserId(linkId) != userId) {
            throw ApiException(LinkStatus.CANNOT_MODIFY_LINK)
        }
    }
}
