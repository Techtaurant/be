package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.lock.DistributedLock
import com.techtaurant.mainserver.link.dto.LinkBatchRunResponse
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
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
    private val tagRepository: TagRepository,
    private val distributedLock: DistributedLock,
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

        val tags = resolveLinkTags(batch.tagNames)
        val result = crawl(batch, tags)
        batch.lastTriggeredAt = Instant.now()

        return result
    }

    private fun crawl(
        batch: LinkCrawlBatch,
        tags: Set<Tag>,
    ): LinkBatchRunResponse {
        var crawlResult = emptyCrawlResult()
        var page = batch.startPage

        while (true) {
            val pageResult = crawlPage(batch, tags, page) ?: break
            crawlResult = crawlResult.mergePageResult(pageResult)

            if (!pageResult.hasNewLinks()) {
                break
            }
            page++
        }

        return crawlResult
    }

    private fun crawlPage(
        batch: LinkCrawlBatch,
        tags: Set<Tag>,
        page: Int,
    ): LinkBatchRunResponse? {
        val pageUrl = buildPageUrl(batch.baseUrl, batch.pageUriTemplate, page)
        val document = fetchPageOrNull(pageUrl) ?: return null
        var pageResult = emptyCrawlResult()

        document.select(batch.itemSelector).forEach { item ->
            pageResult = pageResult.recordCollectionResult(collectLinkItem(item, batch, tags, pageUrl))
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

    private fun LinkBatchRunResponse.recordCollectionResult(result: LinkCollectionResult): LinkBatchRunResponse =
        when (result) {
            LinkCollectionResult.CREATED_NEW_LINK ->
                copy(
                    collectedCount = collectedCount + 1,
                    newLinkCount = newLinkCount + 1,
                )
            LinkCollectionResult.UPDATED_EXISTING_LINK ->
                copy(
                    collectedCount = collectedCount + 1,
                    existingLinkCount = existingLinkCount + 1,
                )
            LinkCollectionResult.SKIPPED ->
                copy(skippedCount = skippedCount + 1)
        }

    private fun LinkBatchRunResponse.mergePageResult(pageResult: LinkBatchRunResponse): LinkBatchRunResponse =
        copy(
            collectedCount = collectedCount + pageResult.collectedCount,
            newLinkCount = newLinkCount + pageResult.newLinkCount,
            existingLinkCount = existingLinkCount + pageResult.existingLinkCount,
            skippedCount = skippedCount + pageResult.skippedCount,
        )

    private fun LinkBatchRunResponse.hasNewLinks(): Boolean = newLinkCount > 0

    private fun collectLinkItem(
        item: Element,
        batch: LinkCrawlBatch,
        tags: Set<Tag>,
        pageUrl: String,
    ): LinkCollectionResult {
        val snapshot = extractSnapshot(item, batch, pageUrl) ?: return LinkCollectionResult.SKIPPED
        return saveNewLinkOrRefreshExistingLink(snapshot, batch, tags)
    }

    private fun saveNewLinkOrRefreshExistingLink(
        snapshot: LinkSnapshot,
        batch: LinkCrawlBatch,
        tags: Set<Tag>,
    ): LinkCollectionResult {
        val existingLink = linkRepository.findByUrl(snapshot.url)
        if (existingLink == null) {
            saveNewLink(snapshot, batch, tags)
            return LinkCollectionResult.CREATED_NEW_LINK
        }

        refreshExistingLink(existingLink, snapshot, tags)
        return LinkCollectionResult.UPDATED_EXISTING_LINK
    }

    private fun saveNewLink(
        snapshot: LinkSnapshot,
        batch: LinkCrawlBatch,
        tags: Set<Tag>,
    ) {
        linkRepository.save(
            Link(
                title = snapshot.title,
                url = snapshot.url,
                summary = snapshot.summary,
                sourceCompanyUser = batch.companyUser,
                publishedAt = snapshot.publishedAt,
                tags = tags.toMutableSet(),
            ),
        )
    }

    private fun refreshExistingLink(
        existingLink: Link,
        snapshot: LinkSnapshot,
        tags: Set<Tag>,
    ) {
        existingLink.title = snapshot.title
        if (snapshot.summary.isNotBlank()) {
            existingLink.summary = snapshot.summary
        }
        if (snapshot.publishedAt != null) {
            existingLink.publishedAt = snapshot.publishedAt
        }
        existingLink.tags.addAll(tags)
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
        val publishedAt = parsePublishedAt(firstResolvedValue(item, batch.publishedAtSelectors))

        return LinkSnapshot(
            title = title,
            url = absoluteUrl,
            summary = summary,
            publishedAt = publishedAt,
        )
    }

    private fun resolveLinkTags(rawTagNames: String?): Set<Tag> {
        val tagNames =
            rawTagNames.toLineList()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()

        if (tagNames.isEmpty()) {
            return emptySet()
        }

        val existingTags = tagRepository.findByNameIn(tagNames)
        val existingTagNames = existingTags.map { it.name }.toSet()
        val newTags =
            tagNames.filter { it !in existingTagNames }
                .map { tagName ->
                    distributedLock.withLockAndTransaction("tag:$tagName") {
                        tagRepository.findByName(tagName)
                            ?: tagRepository.save(Tag(name = tagName))
                    }
                }

        return (existingTags + newTags).toSet()
    }

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
        val publishedAt: Instant?,
    )

    private enum class LinkCollectionResult {
        CREATED_NEW_LINK,
        UPDATED_EXISTING_LINK,
        SKIPPED,
    }
}
