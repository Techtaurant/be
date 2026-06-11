package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
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
    private val linkRepository: LinkRepository = mockk(relaxed = true)
    private val userLinkRepository: UserLinkRepository = mockk(relaxed = true)
    private val tagWriteService: TagWriteService = mockk(relaxed = true)
    private val linkDocumentFetcher = StubLinkDocumentFetcher()
    private val linkBatchRunService =
        LinkBatchRunService(
            linkCrawlBatchRepository = linkCrawlBatchRepository,
            linkRepository = linkRepository,
            userLinkRepository = userLinkRepository,
            tagWriteService = tagWriteService,
            linkDocumentFetcher = linkDocumentFetcher,
        )

    @Test
    @DisplayName("크롤링 가능한 첫 페이지이면 검증을 통과한다")
    fun validateCrawlablePassesWhenFirstPageCanBeCrawled() {
        val batch = createBatch(publishedAtSelectors = ".published-date")
        linkDocumentFetcher.html = crawlableHtml()

        linkBatchRunService.validateCrawlable(batch)

        verify(exactly = 0) { linkRepository.save(any()) }
    }

    @Test
    @DisplayName("첫 페이지에 수집 가능한 항목이 없으면 검증이 실패한다")
    fun validateCrawlableFailsWhenNoItemCanBeCrawled() {
        val batch = createBatch(publishedAtSelectors = ".published-date")
        linkDocumentFetcher.html = "<html><body></body></html>"

        val exception =
            assertFailsWith<ApiException> {
                linkBatchRunService.validateCrawlable(batch)
            }

        assertEquals(LinkStatus.LINK_CRAWL_BATCH_NOT_CRAWLABLE, exception.status)
    }

    @Test
    @DisplayName("검증 중 발행일을 수집할 수 없으면 검증이 실패한다")
    fun validateCrawlableFailsWhenPublishedAtCannotBeCollected() {
        val batch = createBatch(publishedAtSelectors = ".missing-date")
        linkDocumentFetcher.html = crawlableHtml()

        val exception =
            assertFailsWith<ApiException> {
                linkBatchRunService.validateCrawlable(batch)
            }

        assertEquals(LinkStatus.LINK_CRAWL_BATCH_PUBLISHED_AT_REQUIRED, exception.status)
    }

    @Test
    @DisplayName("배치 실행 중 발행일을 수집할 수 없으면 배치를 실패시킨다")
    fun runFailsWhenPublishedAtCannotBeCollected() {
        val batchId = UUID.randomUUID()
        val batch = createBatch(publishedAtSelectors = ".missing-date").apply { id = batchId }
        linkDocumentFetcher.html = crawlableHtml()
        every { linkCrawlBatchRepository.findById(batchId) } returns Optional.of(batch)

        val exception =
            assertFailsWith<ApiException> {
                linkBatchRunService.run(batchId)
            }

        assertEquals(LinkStatus.LINK_CRAWL_BATCH_PUBLISHED_AT_REQUIRED, exception.status)
        verify(exactly = 0) { linkRepository.save(any()) }
    }

    private fun createBatch(publishedAtSelectors: String): LinkCrawlBatch {
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
            publishedAtSelectors = publishedAtSelectors,
            cronExpression = "0 0 * * * *",
            startPage = 1,
            active = true,
            tagNames = "engineering",
        )
    }

    private fun crawlableHtml(): String {
        return """
            <html>
              <body>
                <div class="article-card">
                  <a class="article-link" href="/article/metric-review">
                    <div class="title">Metric Review, 실행을 이끌다</div>
                    <div class="summary">지표 리뷰로 실행 리듬을 만든 이야기입니다.</div>
                    <div class="published-date">2026년 4월 20일</div>
                  </a>
                </div>
              </body>
            </html>
            """.trimIndent()
    }

    private class StubLinkDocumentFetcher : LinkDocumentFetcher {
        var html: String = ""

        override fun fetch(url: String): Document {
            return Jsoup.parse(html, url)
        }
    }
}
