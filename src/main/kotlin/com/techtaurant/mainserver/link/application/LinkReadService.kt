package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.dto.LinkContentDetailResponse
import com.techtaurant.mainserver.link.dto.LinkContentListItemResponse
import com.techtaurant.mainserver.link.dto.LinkCursor
import com.techtaurant.mainserver.link.dto.LinkListItemResponse
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.data.domain.PageRequest
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
        val sourceCompanyUserIdByLinkId = findSourceCompanyUserIdByLinkId(linkPage.content)

        return CursorPageResponse(
            content =
                linkPage.content.map { link ->
                    LinkContentListItemResponse.from(
                        link = link,
                        sourceCompanyUserId = sourceCompanyUserIdByLinkId[link.id],
                    )
                },
            nextCursor = linkPage.nextCursor,
            hasNext = linkPage.hasNext,
            size = linkPage.size,
        )
    }

    fun getPublicLinkContentDetail(linkId: UUID): LinkContentDetailResponse {
        val link =
            linkRepository.findByIdWithTags(linkId)
                ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)

        return LinkContentDetailResponse.from(
            link = link,
            sourceCompanyUserId = findSourceCompanyUserIdByLinkId(listOf(link))[link.id],
        )
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
        val sourceCompanyUserIdByLinkId = findSourceCompanyUserIdByLinkId(contentLinks)
        val savedLinkIds = userLinkRepository.findByUserIdAndLinkIdIn(userId, linkIds).map { it.link.id!! }.toSet()
        val readLinkIds = linkReadLogRepository.findByUserIdAndLinkIdIn(userId, linkIds).map { it.link.id!! }.toSet()

        val content =
            contentLinks.map { link ->
                val linkId = link.id ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)
                LinkListItemResponse.from(
                    link = link,
                    sourceCompanyUserId = sourceCompanyUserIdByLinkId[linkId],
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
        val linkCursor = cursor?.let { LinkCursor.decode(it) }

        if (cursor != null && linkCursor == null) {
            throw ApiException(LinkStatus.INVALID_LINK_CURSOR)
        }

        val normalizedTag = tag?.takeIf { it.isNotBlank() }
        val pageable = PageRequest.of(0, size + 1)
        val linkIds =
            if (linkCursor == null) {
                linkRepository.findFirstPageIds(
                    sourceCompanyUserId = sourceCompanyUserId,
                    tag = normalizedTag,
                    pageable = pageable,
                )
            } else {
                linkRepository.findNextPageIds(
                    sourceCompanyUserId = sourceCompanyUserId,
                    tag = normalizedTag,
                    cursorCreatedAt = linkCursor.createdAt,
                    cursorId = linkCursor.id,
                    pageable = pageable,
                )
            }
        val hasNext = linkIds.size > size
        val contentLinkIds = linkIds.take(size)
        val linksById =
            if (contentLinkIds.isEmpty()) {
                emptyMap()
            } else {
                linkRepository.findAllByIdInWithTags(contentLinkIds).associateBy { it.id }
            }
        val contentLinks = contentLinkIds.mapNotNull(linksById::get)

        val nextCursor =
            if (hasNext && contentLinks.isNotEmpty()) {
                LinkCursor.from(contentLinks.last()).encode()
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

    private fun validateCompany(companyUserId: UUID) {
        val company =
            userRepository.findById(companyUserId).orElseThrow {
                ApiException(UserStatus.COMPANY_NOT_FOUND)
            }

        if (company.role != UserRole.COMPANY) {
            throw ApiException(UserStatus.COMPANY_NOT_FOUND)
        }
    }

    private fun findSourceCompanyUserIdByLinkId(links: List<Link>): Map<UUID, UUID> {
        val linkIds = links.mapNotNull { it.id }
        if (linkIds.isEmpty()) {
            return emptyMap()
        }

        return linkIds.associateWith { linkId ->
            userLinkRepository.findFirstSourceByLinkId(linkId, PageRequest.of(0, 1)).firstOrNull()?.user?.id
                ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)
        }
    }
}
