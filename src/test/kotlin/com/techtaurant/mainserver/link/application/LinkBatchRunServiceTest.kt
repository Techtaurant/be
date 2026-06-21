package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
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
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DisplayName("LinkBatchRunService 테스트")
class LinkBatchRunServiceTest {
    private val linkCrawlBatchRepository: LinkCrawlBatchRepository = mockk()
    private val linkCrawlFailedJobRepository: LinkCrawlFailedJobRepository = mockk(relaxed = true)
    private val linkRepository: LinkRepository = mockk(relaxed = true)
    private val userLinkRepository: UserLinkRepository = mockk(relaxed = true)
    private val tagWriteService: TagWriteService = mockk(relaxed = true)
    private val linkDocumentFetcher = StubLinkDocumentFetcher()
    private val linkBatchRunService =
        LinkBatchRunService(
            linkCrawlBatchRepository = linkCrawlBatchRepository,
            linkCrawlFailedJobRepository = linkCrawlFailedJobRepository,
            linkRepository = linkRepository,
            userLinkRepository = userLinkRepository,
            tagWriteService = tagWriteService,
            linkDocumentFetcher = linkDocumentFetcher,
        )

    @Test
    @DisplayName("크롤링 가능한 첫 페이지이면 검증을 통과한다")
    fun validateCrawlablePassesWhenFirstPageCanBeCrawled() {
        val batch = createBatch(createdAtSelectors = ".created-date")
        linkDocumentFetcher.html = crawlableHtml()

        linkBatchRunService.validateCrawlable(batch)

        verify(exactly = 0) { linkRepository.save(any()) }
    }

    @Test
    @DisplayName("점 구분 생성일이면 크롤링 가능 검증을 통과한다")
    fun validateCrawlablePassesWhenCreatedAtUsesDottedDate() {
        val batch = createBatch(createdAtSelectors = ".created-date")
        linkDocumentFetcher.html = crawlableHtml(createdAtText = "2026. 6. 12")

        linkBatchRunService.validateCrawlable(batch)

        verify(exactly = 0) { linkRepository.save(any()) }
    }

    @Test
    @DisplayName("목록에 생성일이 없으면 아티클 상세 페이지에서 생성일을 수집한다")
    fun validateCrawlablePassesWhenCreatedAtOnlyExistsOnArticlePage() {
        val batch = createBatch(createdAtSelectors = ".created-date")
        linkDocumentFetcher.setHtml("https://example.com/articles?page=1", listHtmlWithoutCreatedAt())
        linkDocumentFetcher.setHtml("https://example.com/article/metric-review", articleDetailHtml())

        linkBatchRunService.validateCrawlable(batch)

        verify(exactly = 0) { linkRepository.save(any()) }
    }

    @Test
    @DisplayName("목록과 상세 페이지 모두 생성일이 없으면 검증이 실패한다")
    fun validateCrawlableFailsWhenCreatedAtMissingOnBothListAndArticlePage() {
        val batch = createBatch(createdAtSelectors = ".created-date")
        linkDocumentFetcher.setHtml("https://example.com/articles?page=1", listHtmlWithoutCreatedAt())
        linkDocumentFetcher.setHtml(
            "https://example.com/article/metric-review",
            "<html><body><div class=\"title\">상세</div></body></html>",
        )

        val exception =
            assertFailsWith<ApiException> {
                linkBatchRunService.validateCrawlable(batch)
            }

        assertEquals(LinkStatus.LINK_CRAWL_BATCH_CREATED_AT_REQUIRED, exception.status)
    }

    @Test
    @DisplayName("첫 페이지에 수집 가능한 항목이 없으면 검증이 실패한다")
    fun validateCrawlableFailsWhenNoItemCanBeCrawled() {
        val batch = createBatch(createdAtSelectors = ".created-date")
        linkDocumentFetcher.html = "<html><body></body></html>"

        val exception =
            assertFailsWith<ApiException> {
                linkBatchRunService.validateCrawlable(batch)
            }

        assertEquals(LinkStatus.LINK_CRAWL_BATCH_NOT_CRAWLABLE, exception.status)
    }

    @Test
    @DisplayName("검증 중 생성일을 수집할 수 없으면 검증이 실패한다")
    fun validateCrawlableFailsWhenCreatedAtCannotBeCollected() {
        val batch = createBatch(createdAtSelectors = ".missing-date")
        linkDocumentFetcher.html = crawlableHtml()

        val exception =
            assertFailsWith<ApiException> {
                linkBatchRunService.validateCrawlable(batch)
            }

        assertEquals(LinkStatus.LINK_CRAWL_BATCH_CREATED_AT_REQUIRED, exception.status)
    }

    @Test
    @DisplayName("배치 실행 중 개별 링크 실패를 기록하고 다음 링크 수집을 계속한다")
    fun runRecordsFailedJobAndContinuesWhenSingleLinkCannotBeProcessed() {
        val batchId = UUID.randomUUID()
        val batch = createBatch(createdAtSelectors = ".created-date").apply { id = batchId }
        val pageUrl = "https://example.com/articles?page=1"
        linkDocumentFetcher.setHtml(pageUrl, mixedCrawlHtml())
        linkDocumentFetcher.setHtml("https://example.com/article/missing-date", articleDetailWithoutCreatedAt())
        every { linkCrawlBatchRepository.findById(batchId) } returns Optional.of(batch)
        every { linkCrawlFailedJobRepository.findByBatchIdAndArticleUrl(batchId, "https://example.com/article/missing-date") } returns null
        every { linkCrawlFailedJobRepository.save(any()) } answers { invocation.args[0] as LinkCrawlFailedJob }
        every { tagWriteService.resolveTags(any()) } returns emptySet()
        every { linkRepository.findByUrl("https://example.com/article/valid") } returns null
        every { linkRepository.save(any<Link>()) } answers {
            (invocation.args[0] as Link).apply { id = UUID.randomUUID() }
        }
        every { userLinkRepository.findByUserIdAndLinkId(any(), any()) } returns null
        every { userLinkRepository.save(any()) } answers { invocation.args[0] as UserLink }

        val response = linkBatchRunService.run(batchId)

        assertEquals(1, response.collectedCount)
        assertEquals(1, response.newLinkCount)
        assertEquals(0, response.existingLinkCount)
        assertEquals(0, response.skippedCount)
        assertEquals(1, response.failedJobCount)
        verify(exactly = 1) { linkRepository.save(any()) }
        verify(exactly = 1) {
            linkCrawlFailedJobRepository.save(
                match {
                    it.batch.id == batchId &&
                        it.sourcePage == 1 &&
                        it.sourcePageUrl == pageUrl &&
                        it.articleUrl == "https://example.com/article/missing-date" &&
                        it.title == "생성일 없는 글" &&
                        it.errorStatusCode == LinkStatus.LINK_CRAWL_BATCH_CREATED_AT_REQUIRED.getCustomStatusCode()
                },
            )
        }
    }

    private fun createBatch(createdAtSelectors: String): LinkCrawlBatch {
        return LinkCrawlBatch(
            companyUser =
                User(
                    name = "토스",
                    email = "company@example.com",
                    provider = OAuthProvider.SYSTEM,
                    identifier = "company",
                    role = UserRole.COMPANY,
                    profileImageUrl = "https://example.com/company.png",
                ).apply { id = UUID.randomUUID() },
            name = "토스 링크 수집",
            baseUrl = "https://example.com",
            pageUriTemplate = "/articles?page={page}",
            itemSelector = ".article-card",
            articleLinkSelector = "a.article-link",
            titleSelector = ".title",
            summarySelector = ".summary",
            createdAtSelectors = createdAtSelectors,
            cronExpression = "0 0 * * * *",
            startPage = 1,
            active = true,
            tagNames = "engineering",
        )
    }

    private fun crawlableHtml(createdAtText: String = "2026년 4월 20일"): String {
        return """
            <html>
              <body>
                <div class="article-card">
                  <a class="article-link" href="/article/metric-review">
                    <div class="title">Metric Review, 실행을 이끌다</div>
                    <div class="summary">지표 리뷰로 실행 리듬을 만든 이야기입니다.</div>
                    <div class="created-date">$createdAtText</div>
                  </a>
                </div>
              </body>
            </html>
            """.trimIndent()
    }

    private fun mixedCrawlHtml(): String {
        return """
            <html>
              <body>
                <div class="article-card">
                  <a class="article-link" href="/article/missing-date">
                    <div class="title">생성일 없는 글</div>
                    <div class="summary">생성일 selector가 맞지 않는 글입니다.</div>
                  </a>
                </div>
                <div class="article-card">
                  <a class="article-link" href="/article/valid">
                    <div class="title">정상 수집 글</div>
                    <div class="summary">정상적으로 수집되는 글입니다.</div>
                    <div class="created-date">2026년 4월 21일</div>
                  </a>
                </div>
              </body>
            </html>
            """.trimIndent()
    }

    private fun articleDetailWithoutCreatedAt(): String {
        return """
            <html>
              <body>
                <h1>생성일 없는 글</h1>
              </body>
            </html>
            """.trimIndent()
    }

    private fun listHtmlWithoutCreatedAt(): String {
        return """
            <html>
              <body>
                <div class="article-card">
                  <a class="article-link" href="/article/metric-review">
                    <div class="title">Metric Review, 실행을 이끌다</div>
                    <div class="summary">지표 리뷰로 실행 리듬을 만든 이야기입니다.</div>
                  </a>
                </div>
              </body>
            </html>
            """.trimIndent()
    }

    private fun articleDetailHtml(createdAtText: String = "2026년 4월 20일"): String {
        return """
            <html>
              <body>
                <h1>Metric Review, 실행을 이끌다</h1>
                <div class="created-date">$createdAtText</div>
              </body>
            </html>
            """.trimIndent()
    }

    private class StubLinkDocumentFetcher : LinkDocumentFetcher {
        var html: String = ""
        private val htmlByUrl = mutableMapOf<String, String>()

        fun setHtml(
            url: String,
            value: String,
        ) {
            htmlByUrl[url] = value
        }

        override fun fetch(url: String): Document {
            return Jsoup.parse(htmlByUrl[url] ?: html, url)
        }
    }
}
