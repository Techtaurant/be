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
import com.techtaurant.mainserver.post.enums.TagTargetType
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
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
        var collectedCount = 0
        var newLinkCount = 0
        var existingLinkCount = 0
        var skippedCount = 0

        for (page in batch.startPage..batch.endPage) {
            val pageUrl = buildPageUrl(batch.baseUrl, batch.pageUriTemplate, page)
            val document = linkDocumentFetcher.fetch(pageUrl)
            val items = document.select(batch.itemSelector)

            items.forEach { item ->
                val snapshot = extractSnapshot(item, batch, pageUrl)
                if (snapshot == null) {
                    skippedCount++
                    return@forEach
                }

                val existingLink = linkRepository.findByUrl(snapshot.url)
                if (existingLink == null) {
                    linkRepository.save(
                        Link(
                            title = snapshot.title,
                            url = snapshot.url,
                            summary = snapshot.summary,
                            sourceCompanyUser = batch.companyUser,
                            authorName = snapshot.authorName,
                            publishedAt = snapshot.publishedAt,
                            tags = tags.toMutableSet(),
                        ),
                    )
                    newLinkCount++
                } else {
                    existingLink.title = snapshot.title
                    if (snapshot.summary.isNotBlank()) {
                        existingLink.summary = snapshot.summary
                    }
                    if (!snapshot.authorName.isNullOrBlank()) {
                        existingLink.authorName = snapshot.authorName
                    }
                    if (snapshot.publishedAt != null) {
                        existingLink.publishedAt = snapshot.publishedAt
                    }
                    existingLink.tags.addAll(tags)
                    existingLinkCount++
                }

                collectedCount++
            }
        }

        return LinkBatchRunResponse(
            collectedCount = collectedCount,
            newLinkCount = newLinkCount,
            existingLinkCount = existingLinkCount,
            skippedCount = skippedCount,
        )
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

        val summary = batch.summarySelector?.let { resolveText(item, it) }.orEmpty().take(80)
        val authorName = firstResolvedValue(item, batch.authorSelectors)
        val publishedAt = parsePublishedAt(firstResolvedValue(item, batch.publishedAtSelectors))

        return LinkSnapshot(
            title = title,
            url = absoluteUrl,
            summary = summary,
            authorName = authorName,
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

        val existingTags = tagRepository.findByNameInAndTargetType(tagNames, TagTargetType.LINK)
        val existingTagNames = existingTags.map { it.name }.toSet()
        val newTags =
            tagNames.filter { it !in existingTagNames }
                .map { tagName ->
                    distributedLock.withLockAndTransaction("link-tag:$tagName") {
                        tagRepository.findByNameAndTargetType(tagName, TagTargetType.LINK)
                            ?: tagRepository.save(Tag(name = tagName, targetType = TagTargetType.LINK))
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
        val authorName: String?,
        val publishedAt: Instant?,
    )
}
