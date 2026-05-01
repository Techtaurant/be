package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.dto.LinkListItemResponse
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.enums.TagTargetType
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LinkReadService(
    private val linkRepository: LinkRepository,
    private val userLinkRepository: UserLinkRepository,
    private val linkReadLogRepository: LinkReadLogRepository,
    private val userRepository: UserRepository,
) {
    companion object {
        private const val CURSOR_DELIMITER = "_"
    }

    fun getCompanyLinks(
        companyUserId: UUID,
        userId: UUID,
        cursor: String?,
        size: Int,
        tag: String?,
    ): CursorPageResponse<LinkListItemResponse> {
        validateCompany(companyUserId)

        val sortedLinks =
            linkRepository.findAllBySourceCompanyUserIdWithTags(companyUserId)
                .filter { link ->
                    tag.isNullOrBlank() ||
                        link.tags.any { candidate ->
                            candidate.targetType == TagTargetType.LINK && candidate.name == tag
                        }
                }.sortedWith(
                    compareByDescending<Link> { it.createdAt.time }
                        .thenByDescending { it.id.toString() },
                )

        val filteredLinks =
            if (cursor == null) {
                sortedLinks
            } else {
                val (lastCreatedAt, lastLinkId) = parseCursor(cursor)
                sortedLinks.filter { link ->
                    link.createdAt.time < lastCreatedAt ||
                        (link.createdAt.time == lastCreatedAt && link.id.toString() < lastLinkId.toString())
                }
            }

        val limit = size + 1
        val pagedLinks = filteredLinks.take(limit)
        val hasNext = pagedLinks.size > size
        val contentLinks = pagedLinks.take(size)
        val linkIds = contentLinks.mapNotNull { it.id }
        val savedLinkIds = userLinkRepository.findByUserIdAndLinkIdIn(userId, linkIds).map { it.link.id!! }.toSet()
        val readLinkIds = linkReadLogRepository.findByUserIdAndLinkIdIn(userId, linkIds).map { it.link.id!! }.toSet()

        val content =
            contentLinks.map { link ->
                val linkId = link.id ?: throw ApiException(com.techtaurant.mainserver.link.enums.LinkStatus.LINK_NOT_FOUND)
                LinkListItemResponse.from(
                    link = link,
                    isSaved = linkId in savedLinkIds,
                    isRead = linkId in readLinkIds,
                )
            }

        val nextCursor =
            if (hasNext && contentLinks.isNotEmpty()) {
                val lastItem = contentLinks.last()
                "${lastItem.createdAt.time}$CURSOR_DELIMITER${lastItem.id}"
            } else {
                null
            }

        return CursorPageResponse(
            content = content,
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    private fun validateCompany(companyUserId: UUID) {
        val company =
            userRepository.findById(companyUserId).orElseThrow {
                ApiException(UserStatus.COMPANY_NOT_FOUND)
            }

        if (company.role != UserRole.COMPANY) {
            throw ApiException(UserStatus.COMPANY_NOT_FOUND)
        }
    }

    private fun parseCursor(cursor: String): Pair<Long, UUID> {
        val parts = cursor.split(CURSOR_DELIMITER, limit = 2)
        require(parts.size == 2) { "Invalid cursor format" }
        return parts[0].toLong() to UUID.fromString(parts[1])
    }
}
