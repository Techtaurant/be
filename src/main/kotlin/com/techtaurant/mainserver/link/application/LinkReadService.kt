package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.link.dto.LinkContentDetailResponse
import com.techtaurant.mainserver.link.dto.LinkContentListItemResponse
import com.techtaurant.mainserver.link.dto.LinkListItemResponse
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.enums.LinkStatus
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

    fun getPublicLinkContents(
        cursor: String?,
        size: Int,
        sourceCompanyUserId: UUID?,
        tag: String?,
    ): CursorPageResponse<LinkContentListItemResponse> {
        val linkPage =
            getLinkPage(
                cursor = cursor,
                size = size,
                sourceCompanyUserId = sourceCompanyUserId,
                tag = tag,
            )

        return CursorPageResponse(
            content = linkPage.content.map(LinkContentListItemResponse::from),
            nextCursor = linkPage.nextCursor,
            hasNext = linkPage.hasNext,
            size = linkPage.size,
        )
    }

    fun getPublicLinkContentDetail(linkId: UUID): LinkContentDetailResponse {
        val link =
            linkRepository.findByIdWithSourceCompanyUserAndTags(linkId)
                ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)

        return LinkContentDetailResponse.from(link)
    }

    fun getCompanyLinks(
        companyUserId: UUID,
        userId: UUID,
        cursor: String?,
        size: Int,
        tag: String?,
    ): CursorPageResponse<LinkListItemResponse> {
        validateCompany(companyUserId)

        val linkPage =
            getLinkPage(
                cursor = cursor,
                size = size,
                sourceCompanyUserId = companyUserId,
                tag = tag,
            )
        val contentLinks = linkPage.content
        val linkIds = contentLinks.mapNotNull { it.id }
        val savedLinkIds = userLinkRepository.findByUserIdAndLinkIdIn(userId, linkIds).map { it.link.id!! }.toSet()
        val readLinkIds = linkReadLogRepository.findByUserIdAndLinkIdIn(userId, linkIds).map { it.link.id!! }.toSet()

        val content =
            contentLinks.map { link ->
                val linkId = link.id ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)
                LinkListItemResponse.from(
                    link = link,
                    isSaved = linkId in savedLinkIds,
                    isRead = linkId in readLinkIds,
                )
            }

        return CursorPageResponse(
            content = content,
            nextCursor = linkPage.nextCursor,
            hasNext = linkPage.hasNext,
            size = content.size,
        )
    }

    private fun getLinkPage(
        cursor: String?,
        size: Int,
        sourceCompanyUserId: UUID?,
        tag: String?,
    ): CursorPageResponse<Link> {
        val parsedCursor = cursor?.let { parseCursor(it) }
        val sortedLinks =
            findLinks(sourceCompanyUserId)
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
            if (parsedCursor == null) {
                sortedLinks
            } else {
                val (lastCreatedAt, lastLinkId) = parsedCursor
                sortedLinks.filter { link ->
                    link.createdAt.time < lastCreatedAt ||
                        (link.createdAt.time == lastCreatedAt && link.id.toString() < lastLinkId.toString())
                }
            }

        val limit = size + 1
        val pagedLinks = filteredLinks.take(limit)
        val hasNext = pagedLinks.size > size
        val contentLinks = pagedLinks.take(size)

        val nextCursor =
            if (hasNext && contentLinks.isNotEmpty()) {
                val lastItem = contentLinks.last()
                "${lastItem.createdAt.time}$CURSOR_DELIMITER${lastItem.id}"
            } else {
                null
            }

        return CursorPageResponse(
            content = contentLinks,
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = contentLinks.size,
        )
    }

    private fun findLinks(sourceCompanyUserId: UUID?): List<Link> =
        if (sourceCompanyUserId == null) {
            linkRepository.findAllWithTags()
        } else {
            linkRepository.findAllBySourceCompanyUserIdWithTags(sourceCompanyUserId)
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
        return runCatching {
            val parts = cursor.split(CURSOR_DELIMITER, limit = 2)
            require(parts.size == 2)
            parts[0].toLong() to UUID.fromString(parts[1])
        }.getOrElse {
            throw ApiException(DefaultStatus.BAD_REQUEST)
        }
    }
}
