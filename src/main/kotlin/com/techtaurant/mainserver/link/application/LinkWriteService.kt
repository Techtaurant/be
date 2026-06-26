package com.techtaurant.mainserver.link.application

import com.github.f4b6a3.uuid.UuidCreator
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.common.util.HtmlSanitizer
import com.techtaurant.mainserver.link.dto.CreateLinkRequest
import com.techtaurant.mainserver.link.dto.LinkContentDetailResponse
import com.techtaurant.mainserver.link.dto.UpdateLinkRequest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.application.TagWriteService
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.util.UUID

@Service
class LinkWriteService(
    private val linkRepository: LinkRepository,
    private val userLinkRepository: UserLinkRepository,
    private val userRepository: UserRepository,
    private val tagWriteService: TagWriteService,
    private val linkDailyStatsService: LinkDailyStatsService,
) {
    @Transactional
    fun createLink(
        userId: UUID,
        request: CreateLinkRequest,
    ): LinkContentDetailResponse {
        val user = findUser(userId)
        val url = normalizeUrl(request.url)
        val existingLink = linkRepository.findByUrl(url)

        val link =
            existingLink
                ?: linkRepository
                    .save(
                        Link(
                            title = sanitizeRequiredTitle(request.title),
                            url = url,
                            summary = sanitizeRequiredSummary(request.summary),
                            tags = resolveLinkTags(request.tags).toMutableSet(),
                        ),
                    ).also { savedLink ->
                        request.createdAt?.let { savedLink.createdAt = it }
                    }

        connectUserToLink(user, link)

        return LinkContentDetailResponse.from(
            link = link,
            sourceCompanyUserId = findFirstSourceUserId(link.id!!),
        )
    }

    @Transactional
    fun updateLink(
        linkId: UUID,
        userId: UUID,
        request: UpdateLinkRequest,
    ): LinkContentDetailResponse {
        val link =
            linkRepository.findByIdWithTags(linkId)
                ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)

        validateFirstSource(linkId, userId)

        request.title?.let { link.title = sanitizeRequiredTitle(it) }
        request.url?.let { requestedUrl ->
            val nextUrl = normalizeUrl(requestedUrl)
            if (nextUrl != link.url) {
                linkRepository.findByUrl(nextUrl)?.let {
                    throw ApiException(LinkStatus.LINK_URL_ALREADY_EXISTS)
                }
                link.url = nextUrl
            }
        }
        request.summary?.let { link.summary = sanitizeRequiredSummary(it) }
        request.tags?.let { link.replaceTags(resolveLinkTags(it)) }
        request.createdAt?.let { link.createdAt = it }

        return LinkContentDetailResponse.from(
            link = link,
            sourceCompanyUserId = findFirstSourceUserId(linkId),
        )
    }

    private fun findUser(userId: UUID): User =
        userRepository.findById(userId).orElseThrow {
            ApiException(UserStatus.USER_NOT_FOUND)
        }

    private fun connectUserToLink(
        user: User,
        link: Link,
    ) {
        val inserted =
            userLinkRepository.insertIfAbsent(
                id = UuidCreator.getTimeOrderedEpoch(),
                userId = user.id!!,
                linkId = link.id!!,
            )

        if (inserted == 1) {
            linkDailyStatsService.incrementSaveCount(link.id!!, DateUtils.today())
        }
    }

    private fun validateFirstSource(
        linkId: UUID,
        userId: UUID,
    ) {
        if (findFirstSourceUserId(linkId) != userId) {
            throw ApiException(LinkStatus.CANNOT_MODIFY_LINK)
        }
    }

    private fun findFirstSourceUserId(linkId: UUID): UUID =
        userLinkRepository.findFirstSourceByLinkId(linkId, PageRequest.of(0, 1)).firstOrNull()?.user?.id
            ?: throw ApiException(LinkStatus.LINK_NOT_FOUND)

    private fun sanitizeRequiredTitle(title: String): String {
        val sanitizedTitle = HtmlSanitizer.sanitizeTitle(title).trim()
        if (sanitizedTitle.isBlank()) {
            throw ApiException(LinkStatus.LINK_TITLE_REQUIRED)
        }
        return sanitizedTitle
    }

    private fun sanitizeRequiredSummary(summary: String): String {
        val sanitizedSummary = HtmlSanitizer.sanitizeTitle(summary).trim()
        if (sanitizedSummary.isBlank()) {
            throw ApiException(LinkStatus.LINK_SUMMARY_REQUIRED)
        }
        return sanitizedSummary
    }

    private fun resolveLinkTags(tagNames: List<String>?): Set<Tag> = tagWriteService.resolveTags(tagNames.orEmpty().map(String::lowercase))

    private fun normalizeUrl(url: String): String {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            throw ApiException(LinkStatus.LINK_URL_REQUIRED)
        }

        val uri =
            runCatching { URI.create(normalizedUrl) }.getOrNull()
                ?: throw ApiException(LinkStatus.INVALID_LINK_URL)
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw ApiException(LinkStatus.INVALID_LINK_URL)
        }
        if (uri.host.isNullOrBlank()) {
            throw ApiException(LinkStatus.INVALID_LINK_URL)
        }

        return normalizedUrl
    }
}
