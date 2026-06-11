package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.dto.LinkBatchRunResponse
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.application.TagWriteService
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.user.entity.User
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@Service
class LinkBatchRunService(
    private val linkCrawlBatchRepository: LinkCrawlBatchRepository,
    private val linkRepository: LinkRepository,
    private val userLinkRepository: UserLinkRepository,
    private val tagWriteService: TagWriteService,
    private val linkDocumentFetcher: LinkDocumentFetcher,
) {
    companion object {
        private val KOREAN_DATE_REGEX = Regex("""^\s*(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일\s*$""")
    }

    @Transactional
    fun run(batchId: UUID): LinkBatchRunResponse {
        val batch =
            linkCrawlBatchRepository.findById(batchId).orElseThrow {
                ApiException(LinkStatus.LINK_CRAWL_BATCH_NOT_FOUND)
            }

        val tagResolver = LinkTagResolver(resolveLinkTagNames(batch.tagNames), tagWriteService::resolveTags)
        val result = crawl(batch, tagResolver)
        batch.lastTriggeredAt = Instant.now()

        return result
    }

    fun validateCrawlable(batch: LinkCrawlBatch) {
        val pageUrl = buildPageUrl(batch.baseUrl, batch.pageUriTemplate, batch.startPage)
        val document = fetchPageOrNull(pageUrl) ?: throw ApiException(LinkStatus.LINK_CRAWL_BATCH_NOT_CRAWLABLE)
        val hasCrawlableItem =
            document.select(batch.itemSelector)
                .any { item -> extractSnapshot(item, batch, pageUrl) != null }

        if (!hasCrawlableItem) {
            throw ApiException(LinkStatus.LINK_CRAWL_BATCH_NOT_CRAWLABLE)
        }
    }

    private fun crawl(
        batch: LinkCrawlBatch,
        tagResolver: LinkTagResolver,
    ): LinkBatchRunResponse {
        var crawlResult = emptyCrawlResult()
        var page = batch.startPage

        while (true) {
            val pageResult = crawlPage(batch, tagResolver, page) ?: break
            crawlResult = crawlResult.mergePageResult(pageResult.response)

            if (!pageResult.hasProgress) {
                break
            }
            page++
        }

        return crawlResult
    }

    private fun crawlPage(
        batch: LinkCrawlBatch,
        tagResolver: LinkTagResolver,
        page: Int,
    ): LinkPageCrawlResult? {
        val pageUrl = buildPageUrl(batch.baseUrl, batch.pageUriTemplate, page)
        val document = fetchPageOrNull(pageUrl) ?: return null
        var pageResult = emptyPageCrawlResult()

        document.select(batch.itemSelector).forEach { item ->
            pageResult = pageResult.recordCollectionResult(collectLinkItem(item, batch, tagResolver, pageUrl))
        }

        return pageResult
    }

    private fun emptyCrawlResult(): LinkBatchRunResponse =
        LinkBatchRunResponse(
            collectedCount = 0,
            newLinkCount = 0,
            existingLinkCount = 0,
            skippedCount = 0,
        )

    private fun emptyPageCrawlResult(): LinkPageCrawlResult =
        LinkPageCrawlResult(
            response = emptyCrawlResult(),
            hasProgress = false,
        )

    private fun LinkPageCrawlResult.recordCollectionResult(result: LinkCollectionResult): LinkPageCrawlResult {
        val updatedResponse =
            when (result) {
                LinkCollectionResult.CREATED_NEW_LINK ->
                    response.copy(
                        collectedCount = response.collectedCount + 1,
                        newLinkCount = response.newLinkCount + 1,
                    )
                LinkCollectionResult.CONNECTED_EXISTING_LINK,
                LinkCollectionResult.UPDATED_EXISTING_LINK,
                ->
                    response.copy(
                        collectedCount = response.collectedCount + 1,
                        existingLinkCount = response.existingLinkCount + 1,
                    )
                LinkCollectionResult.SKIPPED ->
                    response.copy(skippedCount = response.skippedCount + 1)
            }

        return copy(
            response = updatedResponse,
            hasProgress = hasProgress || result.hasProgress,
        )
    }

    private fun LinkBatchRunResponse.mergePageResult(pageResult: LinkBatchRunResponse): LinkBatchRunResponse =
        copy(
            collectedCount = collectedCount + pageResult.collectedCount,
            newLinkCount = newLinkCount + pageResult.newLinkCount,
            existingLinkCount = existingLinkCount + pageResult.existingLinkCount,
            skippedCount = skippedCount + pageResult.skippedCount,
        )

    private fun collectLinkItem(
        item: Element,
        batch: LinkCrawlBatch,
        tagResolver: LinkTagResolver,
        pageUrl: String,
    ): LinkCollectionResult {
        val snapshot = extractSnapshot(item, batch, pageUrl) ?: return LinkCollectionResult.SKIPPED
        return saveNewLinkOrRefreshExistingLink(snapshot, batch, tagResolver)
    }

    private fun saveNewLinkOrRefreshExistingLink(
        snapshot: LinkSnapshot,
        batch: LinkCrawlBatch,
        tagResolver: LinkTagResolver,
    ): LinkCollectionResult {
        val existingLink = linkRepository.findByUrl(snapshot.url)
        if (existingLink == null) {
            val savedLink = saveNewLink(snapshot, tagResolver.resolve())
            connectUserToLink(batch.companyUser, savedLink)
            return LinkCollectionResult.CREATED_NEW_LINK
        }

        refreshExistingLink(existingLink, snapshot)
        val isConnected = connectUserToLink(batch.companyUser, existingLink)
        return if (isConnected) {
            LinkCollectionResult.CONNECTED_EXISTING_LINK
        } else {
            LinkCollectionResult.UPDATED_EXISTING_LINK
        }
    }

    private fun saveNewLink(
        snapshot: LinkSnapshot,
        tags: Set<Tag>,
    ): Link {
        return linkRepository.save(
            Link(
                title = snapshot.title,
                url = snapshot.url,
                summary = snapshot.summary,
                publishedAt = snapshot.publishedAt,
            ).apply { replaceTags(tags) },
        )
    }

    private fun refreshExistingLink(
        existingLink: Link,
        snapshot: LinkSnapshot,
    ) {
        existingLink.title = snapshot.title
        if (snapshot.summary.isNotBlank()) {
            existingLink.summary = snapshot.summary
        }
        existingLink.publishedAt = snapshot.publishedAt
    }

    private fun connectUserToLink(
        user: User,
        link: Link,
    ): Boolean {
        val userId = user.id!!
        val linkId = link.id!!

        if (userLinkRepository.findByUserIdAndLinkId(userId, linkId) == null) {
            userLinkRepository.save(UserLink(user = user, link = link))
            return true
        }

        return false
    }

    private fun fetchPageOrNull(pageUrl: String): Document? {
        return runCatching { linkDocumentFetcher.fetch(pageUrl) }
            .getOrElse { exception ->
                if (exception is HttpStatusException) {
                    null
                } else {
                    throw exception
                }
            }
    }

    private fun extractSnapshot(
        item: Element,
        batch: LinkCrawlBatch,
        pageUrl: String,
    ): LinkSnapshot? {
        val linkElement = resolveElement(item, batch.articleLinkSelector) ?: return null
        val href = linkElement.attr("href").trim()
        if (href.isBlank()) {
            return null
        }

        val absoluteUrl =
            linkElement.absUrl("href").ifBlank {
                URI.create(pageUrl).resolve(href).toString()
            }

        val title =
            resolveText(item, batch.titleSelector)
                ?.takeIf { it.isNotBlank() }
                ?: return null

        val summary = batch.summarySelector?.let { resolveText(item, it) }.orEmpty()
        val publishedAt =
            parsePublishedAt(firstResolvedValue(item, batch.publishedAtSelectors))
                ?: throw ApiException(LinkStatus.LINK_CRAWL_BATCH_PUBLISHED_AT_REQUIRED)

        return LinkSnapshot(
            title = title,
            url = absoluteUrl,
            summary = summary,
            publishedAt = publishedAt,
        )
    }

    private fun resolveLinkTagNames(rawTagNames: String?): List<String> =
        rawTagNames.toLineList()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()

    private fun buildPageUrl(
        baseUrl: String,
        pageUriTemplate: String,
        page: Int,
    ): String {
        val pageUri = pageUriTemplate.replace("{page}", page.toString())
        return if (pageUri.startsWith("http://") || pageUri.startsWith("https://")) {
            pageUri
        } else {
            URI.create(baseUrl).resolve(pageUri).toString()
        }
    }

    private fun resolveElement(
        root: Element,
        selector: String?,
    ): Element? {
        if (selector.isNullOrBlank()) {
            return null
        }

        return if (selector.trim() == ":self") {
            root
        } else {
            root.selectFirst(selector)
        }
    }

    private fun resolveText(
        root: Element,
        selector: String?,
    ): String? {
        return resolveElement(root, selector)?.text()?.trim()
    }

    private fun firstResolvedValue(
        root: Element,
        selectors: String?,
    ): String? {
        return selectors.toLineList()
            .mapNotNull { selector ->
                val element = resolveElement(root, selector)
                val value =
                    element?.attr("datetime")?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: element?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
                        ?: element?.text()?.trim()?.takeIf { it.isNotEmpty() }
                value
            }.firstOrNull()
    }

    private fun parsePublishedAt(rawValue: String?): Instant? {
        if (rawValue.isNullOrBlank()) {
            return null
        }

        return runCatching { Instant.parse(rawValue) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(rawValue).toInstant() }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(rawValue).toInstant() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(rawValue).toInstant(ZoneOffset.UTC) }.getOrNull()
            ?: runCatching { LocalDate.parse(rawValue).atStartOfDay().toInstant(ZoneOffset.UTC) }.getOrNull()
            ?: parseKoreanDate(rawValue)
    }

    private fun parseKoreanDate(rawValue: String): Instant? {
        val match = KOREAN_DATE_REGEX.matchEntire(rawValue) ?: return null
        val (year, month, day) = match.destructured

        return runCatching {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
        }.getOrNull()
    }

    private fun String?.toLineList(): List<String> {
        return this?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toList()
            ?: emptyList()
    }

    private data class LinkSnapshot(
        val title: String,
        val url: String,
        val summary: String,
        val publishedAt: Instant,
    )

    private data class LinkPageCrawlResult(
        val response: LinkBatchRunResponse,
        val hasProgress: Boolean,
    )

    private enum class LinkCollectionResult(
        val hasProgress: Boolean,
    ) {
        CREATED_NEW_LINK(true),
        CONNECTED_EXISTING_LINK(true),
        UPDATED_EXISTING_LINK(false),
        SKIPPED(false),
    }

    private class LinkTagResolver(
        private val tagNames: List<String>,
        private val resolveTags: (Collection<String>) -> Set<Tag>,
    ) {
        private var resolvedTags: Set<Tag>? = null

        fun resolve(): Set<Tag> {
            if (tagNames.isEmpty()) {
                return emptySet()
            }
            resolvedTags?.let { return it }
            return resolveTags(tagNames).also { resolvedTags = it }
        }
    }
}
