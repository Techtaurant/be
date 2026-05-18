package com.techtaurant.mainserver.link.infrastructure.`in`

import com.sun.net.httpserver.HttpServer
import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("AdminLinkCrawlBatchController 통합 테스트")
class AdminLinkCrawlBatchControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var linkCrawlBatchRepository: LinkCrawlBatchRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminUser: User
    private lateinit var companyUser: User
    private lateinit var adminAccessToken: String
    private lateinit var httpServer: HttpServer
    private lateinit var crawlerBaseUrl: String
    private val pageRequestCounts = ConcurrentHashMap<Int, AtomicInteger>()

    @BeforeEach
    fun setUpTestData() {
        userRepository.deleteAllInBatch()
        pageRequestCounts.clear()

        adminUser =
            userRepository.save(
                User(
                    name = "관리자",
                    email = "admin-${UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "admin-id-${UUID.randomUUID()}",
                    role = UserRole.ADMIN,
                    profileImageUrl = "https://example.com/admin-profile.jpg",
                ),
            )

        companyUser =
            userRepository.save(
                User(
                    name = "토스",
                    email = "contact@toss.im",
                    provider = OAuthProvider.SYSTEM,
                    identifier = "company-toss",
                    role = UserRole.COMPANY,
                    profileImageUrl = "https://example.com/toss.png",
                ),
            )

        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id!!, adminUser.role)

        httpServer = HttpServer.create(InetSocketAddress(0), 0)
        httpServer.createContext("/category/engineering") { exchange ->
            val page = resolvePage(exchange.requestURI.query)
            pageRequestCounts.computeIfAbsent(page) { AtomicInteger() }.incrementAndGet()

            val html =
                when (page) {
                    1 ->
                        """
                        <html>
                          <body>
                            <div class="article-card">
                              <a class="article-link" href="/article/metric-review">
                                <div class="title">Metric Review, 실행을 이끌다</div>
                                <div class="summary">인사이트는 있는데 실행이 느릴 때, 지표 리뷰로 실행 리듬을 만든 이야기입니다.</div>
                                <div class="author">박종익</div>
                                <div class="published-date o6bzluc">2026년 4월 20일</div>
                              </a>
                            </div>
                            <div class="article-card">
                              <a class="article-link" href="/article/starrocks">
                                <div class="title">StarRocks 운영기</div>
                                <div class="summary">서비스 쿼리가 밀리기 시작했을 때 우리가 선택한 멀티테넌트 격리 전략을 정리했습니다.</div>
                                <div class="author">이유진</div>
                                <div class="published-date o6bzluc">2026년 4월 19일</div>
                              </a>
                            </div>
                          </body>
                        </html>
                        """.trimIndent()
                    2 ->
                        """
                        <html>
                          <body>
                            <div class="article-card">
                              <a class="article-link" href="/article/cache-layer">
                                <div class="title">Cache Layer 개선기</div>
                                <div class="summary">반복 조회 부하를 낮추기 위해 캐시 계층을 재설계한 경험을 정리했습니다.</div>
                                <div class="author">김도현</div>
                                <div class="published-date o6bzluc">2026년 4월 18일</div>
                              </a>
                            </div>
                          </body>
                        </html>
                        """.trimIndent()
                    else -> null
                }

            if (html == null) {
                exchange.sendResponseHeaders(HttpStatus.NOT_FOUND.value(), -1)
                exchange.close()
                return@createContext
            }

            val bytes = html.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(HttpStatus.OK.value(), bytes.size.toLong())
            exchange.responseBody.use { body -> body.write(bytes) }
        }
        httpServer.start()

        crawlerBaseUrl = "http://localhost:${httpServer.address.port}"
    }

    @AfterEach
    fun tearDownServer() {
        httpServer.stop(0)
    }

    @Test
    @DisplayName("관리자는 회사 배치를 등록하고 수동 실행으로 링크를 수집할 수 있다")
    fun adminCanCreateBatchAndRunManually() {
        val batchId =
            given()
                .contentType("application/json")
                .header("Authorization", "Bearer $adminAccessToken")
                .body(
                    """
                    {
                      "name": "토스 엔지니어링 링크 수집",
                      "baseUrl": "$crawlerBaseUrl",
                      "pageUriTemplate": "/category/engineering?page={page}",
                      "itemSelector": ".article-card",
                      "articleLinkSelector": "a.article-link",
                      "titleSelector": ".title",
                      "summarySelector": ".summary",
                      "publishedAtSelectors": ["div.o6bzluc"],
                      "tagNames": ["engineering", "backend"],
                      "cronExpression": "0 0 * * * *",
                      "startPage": 1,
                      "active": true
                    }
                    """.trimIndent(),
                ).`when`()
                .post("/admin/companies/${companyUser.id}/link-crawl-batches")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("data.name", equalTo("토스 엔지니어링 링크 수집"))
                .body("data.tagNames", hasSize<Any>(2))
                .extract()
                .path<String>("data.id")

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .post("/admin/link-crawl-batches/$batchId/run")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.collectedCount", equalTo(3))
            .body("data.newLinkCount", equalTo(3))
            .body("data.existingLinkCount", equalTo(0))
            .body("data.skippedCount", equalTo(0))

        val savedLinks = linkRepository.findAllWithTags()
        assertEquals(3, savedLinks.size)
        assertTrue(savedLinks.all { it.sourceCompanyUser.id == companyUser.id })
        assertTrue(savedLinks.all { it.tags.map { tag -> tag.name }.containsAll(listOf("engineering", "backend")) })
        assertEquals(
            "2026-04-20T00:00:00Z",
            savedLinks.first { it.url.endsWith("/article/metric-review") }.publishedAt.toString(),
        )

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .post("/admin/link-crawl-batches/$batchId/run")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.collectedCount", equalTo(2))
            .body("data.newLinkCount", equalTo(0))
            .body("data.existingLinkCount", equalTo(2))

        assertEquals(2, pageRequestCount(1))
        assertEquals(1, pageRequestCount(2))
    }

    @Test
    @DisplayName("관리자는 회사 배치를 조회하고 수정할 수 있다")
    fun adminCanListAndUpdateCompanyBatches() {
        val batch =
            linkCrawlBatchRepository.save(
                com.techtaurant.mainserver.link.entity.LinkCrawlBatch(
                    companyUser = companyUser,
                    name = "초기 배치",
                    baseUrl = crawlerBaseUrl,
                    pageUriTemplate = "/category/engineering?page={page}",
                    itemSelector = ".article-card",
                    articleLinkSelector = "a.article-link",
                    titleSelector = ".title",
                    summarySelector = ".summary",
                    publishedAtSelectors = "time",
                    cronExpression = "0 0 * * * *",
                    startPage = 1,
                    active = true,
                    tagNames = "engineering\nbackend",
                ),
            )

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .get("/admin/companies/${companyUser.id}/link-crawl-batches")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(1))
            .body("data[0].name", equalTo("초기 배치"))

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $adminAccessToken")
            .body(
                """
                {
                  "name": "수정된 배치",
                  "active": false,
                  "tagNames": ["infra"]
                }
                """.trimIndent(),
            ).`when`()
            .patch("/admin/link-crawl-batches/${batch.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.name", equalTo("수정된 배치"))
            .body("data.active", equalTo(false))
            .body("data.tagNames", hasSize<Any>(1))
            .body("data.tagNames[0]", equalTo("infra"))
    }

    private fun resolvePage(query: String?): Int {
        return query?.split("&")
            ?.mapNotNull { parameter ->
                val parts = parameter.split("=", limit = 2)
                if (parts.firstOrNull() == "page") {
                    parts.getOrNull(1)?.toIntOrNull()
                } else {
                    null
                }
            }?.firstOrNull()
            ?: 1
    }

    private fun pageRequestCount(page: Int): Int {
        return pageRequestCounts[page]?.get() ?: 0
    }
}
