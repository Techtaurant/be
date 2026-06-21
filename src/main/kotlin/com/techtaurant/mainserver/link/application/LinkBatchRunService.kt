package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.link.dto.LinkBatchRunResponse
import com.techtaurant.mainserver.link.dto.LinkCrawlFailedJobResponse
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import com.techtaurant.mainserver.link.entity.LinkCrawlFailedJob
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlFailedJobRepository
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
    private val linkCrawlFailedJobRepository: LinkCrawlFailedJobRepository,
    private val linkRepository: LinkRepository,
    private val userLinkRepository: UserLinkRepository,
    private val tagWriteService: TagWriteService,
    private val linkDocumentFetcher: LinkDocumentFetcher,
) {
    companion object {
        private val ABSOLUTE_DATE_REGEX = Regex("""^\s*(\d{4})\s*(?:[./-]|년)\s*(\d{1,2})\s*(?:[./-]|월)\s*(\d{1,2})\s*(?:일)?\s*\.?\s*$""")
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

    @Transactional(readOnly = true)
    fun getFailedJobs(batchId: UUID): List<LinkCrawlFailedJobResponse> {
        if (!linkCrawlBatchRepository.existsById(batchId)) {
            throw ApiException(LinkStatus.LINK_CRAWL_BATCH_NOT_FOUND)
        }

        return linkCrawlFailedJobRepository.findAllByBatchIdOrderByCreatedAtAsc(batchId)
            .map(LinkCrawlFailedJobResponse::from)
    }

    @Transactional
    fun runFailedJob(failedJobId: UUID): LinkBatchRunResponse {
        val failedJob = getFailedJob(failedJobId)
        val tagResolver = LinkTagResolver(resolveLinkTagNames(failedJob.batch.tagNames), tagWriteService::resolveTags)

        return runCatching {
            val snapshot = resolveSnapshotForFailedJob(failedJob)
            val result = saveNewLinkOrRefreshExistingLink(snapshot, failedJob.batch, tagResolver)
            linkCrawlFailedJobRepository.delete(failedJob)
            emptyPageCrawlResult().recordCollectionResult(result).response
        }.getOrElse { exception ->
            recordFailedJob(
                batch = failedJob.batch,
                failedJobDraft = failedJob.toDraft(),
                sourcePage = failedJob.sourcePage,
                sourcePageUrl = failedJob.sourcePageUrl,
                exception = exception,
            )
            emptyCrawlResult().copy(failedJobCount = 1)
        }
    }

    @Transactional
    fun deleteFailedJob(failedJobId: UUID) {
        linkCrawlFailedJobRepository.delete(getFailedJob(failedJobId))
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
            pageResult = pageResult.recordCollectionResult(collectLinkFromCrawledItem(item, batch, tagResolver, page, pageUrl))
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
                LinkCollectionResult.FAILED ->
                    response.copy(failedJobCount = response.failedJobCount + 1)
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
            failedJobCount = failedJobCount + pageResult.failedJobCount,
        )

    private fun collectLinkFromCrawledItem(
        item: Element,
        batch: LinkCrawlBatch,
        tagResolver: LinkTagResolver,
        page: Int,
        pageUrl: String,
    ): LinkCollectionResult {
        val snapshot =
            try {
                extractSnapshot(item, batch, pageUrl) ?: return LinkCollectionResult.SKIPPED
            } catch (exception: Exception) {
                val failedJobDraft = extractFailedJobDraft(item, batch, pageUrl) ?: return LinkCollectionResult.SKIPPED
                recordFailedJob(batch, failedJobDraft, page, pageUrl, exception)
                return LinkCollectionResult.FAILED
            }

        return runCatching {
            saveNewLinkOrRefreshExistingLink(snapshot, batch, tagResolver)
        }.getOrElse { exception ->
            recordFailedJob(batch, snapshot.toFailedJobDraft(), page, pageUrl, exception)
            LinkCollectionResult.FAILED
        }
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
                createdAt = snapshot.createdAt,
            ).apply {
                replaceTags(tags)
            },
        ).also { savedLink ->
            savedLink.createdAt = snapshot.createdAt
        }
    }

    private fun refreshExistingLink(
        existingLink: Link,
        snapshot: LinkSnapshot,
    ) {
        existingLink.title = snapshot.title
        if (snapshot.summary.isNotBlank()) {
            existingLink.summary = snapshot.summary
        }
        existingLink.createdAt = snapshot.createdAt
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

    private fun getFailedJob(failedJobId: UUID): LinkCrawlFailedJob {
        return linkCrawlFailedJobRepository.findById(failedJobId).orElseThrow {
            ApiException(LinkStatus.LINK_CRAWL_FAILED_JOB_NOT_FOUND)
        }
    }

    private fun resolveSnapshotForFailedJob(failedJob: LinkCrawlFailedJob): LinkSnapshot {
        val batch = failedJob.batch
        val snapshotFromSourcePage =
            fetchPageOrNull(failedJob.sourcePageUrl)
                ?.select(batch.itemSelector)
                ?.firstNotNullOfOrNull { item ->
                    val articleUrl = extractArticleUrl(item, batch, failedJob.sourcePageUrl)
                    if (articleUrl == failedJob.articleUrl) {
                        extractSnapshot(item, batch, failedJob.sourcePageUrl)
                    } else {
                        null
                    }
                }

        if (snapshotFromSourcePage != null) {
            return snapshotFromSourcePage
        }

        val title =
            failedJob.title?.trim()?.takeIf(String::isNotEmpty)
                ?: throw ApiException(LinkStatus.LINK_CRAWL_BATCH_NOT_CRAWLABLE)
        val createdAt =
            parseCreatedAtFromArticlePage(failedJob.articleUrl, batch.createdAtSelectors)
                ?: throw ApiException(LinkStatus.LINK_CRAWL_BATCH_CREATED_AT_REQUIRED)

        return LinkSnapshot(
            title = title,
            url = failedJob.articleUrl,
            summary = failedJob.summary.orEmpty(),
            createdAt = createdAt,
        )
    }

    private fun recordFailedJob(
        batch: LinkCrawlBatch,
        failedJobDraft: LinkFailedJobDraft,
        sourcePage: Int,
        sourcePageUrl: String,
        exception: Throwable,
    ) {
        val batchId = batch.id ?: throw IllegalStateException("배치 ID가 없습니다")
        val now = Instant.now()
        val errorStatusCode = exception.toErrorStatusCode()
        val errorMessage = exception.toErrorMessage()
        val failedJob =
            linkCrawlFailedJobRepository.findByBatchIdAndArticleUrl(batchId, failedJobDraft.articleUrl)
                ?.apply {
                    this.sourcePage = sourcePage
                    this.sourcePageUrl = sourcePageUrl
                    this.title = failedJobDraft.title
                    this.summary = failedJobDraft.summary
                    this.errorStatusCode = errorStatusCode
                    this.errorMessage = errorMessage
                    this.failureCount += 1
                    this.lastFailedAt = now
                }
                ?: LinkCrawlFailedJob(
                    batch = batch,
                    sourcePage = sourcePage,
                    sourcePageUrl = sourcePageUrl,
                    articleUrl = failedJobDraft.articleUrl,
                    title = failedJobDraft.title,
                    summary = failedJobDraft.summary,
                    errorStatusCode = errorStatusCode,
                    errorMessage = errorMessage,
                    lastFailedAt = now,
                )

        linkCrawlFailedJobRepository.save(failedJob)
    }

    private fun Throwable.toErrorStatusCode(): Int {
        return if (this is ApiException) {
            status.getCustomStatusCode()
        } else {
            DefaultStatus.UNKNOWN_EXCEPTION.getCustomStatusCode()
        }
    }

    private fun Throwable.toErrorMessage(): String {
        return if (this is ApiException) {
            detail
        } else {
            message ?: javaClass.simpleName
        }
    }

    private fun extractArticleUrl(
        item: Element,
        batch: LinkCrawlBatch,
        pageUrl: String,
    ): String? {
        val linkElement = resolveElement(item, batch.articleLinkSelector) ?: return null
        val href = linkElement.attr("href").trim()
        if (href.isBlank()) {
            return null
        }

        return linkElement.absUrl("href").ifBlank {
            URI.create(pageUrl).resolve(href).toString()
        }
    }

    private fun extractFailedJobDraft(
        item: Element,
        batch: LinkCrawlBatch,
        pageUrl: String,
    ): LinkFailedJobDraft? {
        val articleUrl = extractArticleUrl(item, batch, pageUrl) ?: return null
        val title = resolveText(item, batch.titleSelector)?.trim()?.takeIf(String::isNotEmpty)?.take(200)
        val summary = batch.summarySelector?.let { resolveText(item, it) }?.trim()?.takeIf(String::isNotEmpty)

        return LinkFailedJobDraft(
            articleUrl = articleUrl,
            title = title,
            summary = summary,
        )
    }

    private fun extractSnapshot(
        item: Element,
        batch: LinkCrawlBatch,
        pageUrl: String,
    ): LinkSnapshot? {
        val absoluteUrl = extractArticleUrl(item, batch, pageUrl) ?: return null

        val title =
            resolveText(item, batch.titleSelector)
                ?.takeIf { it.isNotBlank() }
                ?: return null

        val summary = batch.summarySelector?.let { resolveText(item, it) }.orEmpty()
        val createdAt =
            resolveCreatedAt(item, absoluteUrl, batch)
                ?: throw ApiException(LinkStatus.LINK_CRAWL_BATCH_CREATED_AT_REQUIRED)

        return LinkSnapshot(
            title = title,
            url = absoluteUrl,
            summary = summary,
            createdAt = createdAt,
        )
    }

    /**
     * 생성일을 목록 카드에서 먼저 찾고, 없으면 아티클 상세 페이지를 조회해 추출한다.
     * 토스테크처럼 목록에는 날짜가 없고 상세 페이지에만 날짜가 있는 블로그를 지원한다.
     *
     * @param item 목록 페이지에서 선택된 카드 엘리먼트
     * @param articleUrl 카드에서 추출한 아티클 상세 페이지의 절대 URL
     * @param batch 생성일 셀렉터를 포함한 크롤 배치 설정
     * @return 파싱된 생성일, 목록과 상세 페이지 어디에서도 찾지 못하면 null
     */
    private fun resolveCreatedAt(
        item: Element,
        articleUrl: String,
        batch: LinkCrawlBatch,
    ): Instant? {
        val createdAtFromListItem = parseCreatedAt(firstResolvedValue(item, batch.createdAtSelectors))
        if (createdAtFromListItem != null) {
            return createdAtFromListItem
        }

        return parseCreatedAtFromArticlePage(articleUrl, batch.createdAtSelectors)
    }

    /**
     * 아티클 상세 페이지를 조회해 생성일을 추출한다.
     *
     * @param articleUrl 조회할 아티클 상세 페이지의 절대 URL
     * @param createdAtSelectors 줄바꿈으로 구분된 생성일 셀렉터 목록
     * @return 파싱된 생성일, 셀렉터가 비었거나 상세 페이지 조회/파싱에 실패하면 null
     */
    private fun parseCreatedAtFromArticlePage(
        articleUrl: String,
        createdAtSelectors: String?,
    ): Instant? {
        if (createdAtSelectors.isNullOrBlank()) {
            return null
        }

        val articleDocument = fetchPageOrNull(articleUrl) ?: return null
        return parseCreatedAt(firstResolvedValue(articleDocument, createdAtSelectors))
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

    private fun parseCreatedAt(rawValue: String?): Instant? {
        if (rawValue.isNullOrBlank()) {
            return null
        }

        return runCatching { Instant.parse(rawValue) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(rawValue).toInstant() }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(rawValue).toInstant() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(rawValue).toInstant(ZoneOffset.UTC) }.getOrNull()
            ?: runCatching { LocalDate.parse(rawValue).atStartOfDay().toInstant(ZoneOffset.UTC) }.getOrNull()
            ?: parseAbsoluteDate(rawValue)
    }

    private fun parseAbsoluteDate(rawValue: String): Instant? {
        val match = ABSOLUTE_DATE_REGEX.matchEntire(rawValue) ?: return null
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
        val createdAt: Instant,
    ) {
        fun toFailedJobDraft(): LinkFailedJobDraft =
            LinkFailedJobDraft(
                articleUrl = url,
                title = title.take(200),
                summary = summary.takeIf(String::isNotBlank),
            )
    }

    private data class LinkFailedJobDraft(
        val articleUrl: String,
        val title: String?,
        val summary: String?,
    )

    private fun LinkCrawlFailedJob.toDraft(): LinkFailedJobDraft =
        LinkFailedJobDraft(
            articleUrl = articleUrl,
            title = title,
            summary = summary,
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
        FAILED(false),
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
