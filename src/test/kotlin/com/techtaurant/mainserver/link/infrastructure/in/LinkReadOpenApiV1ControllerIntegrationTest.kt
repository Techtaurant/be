package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@DisplayName("LinkReadOpenApiV1Controller 통합 테스트")
class LinkReadOpenApiV1ControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var userLinkRepository: UserLinkRepository

    @Autowired
    private lateinit var linkDailyStatsRepository: LinkDailyStatsRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var company: User

    @BeforeEach
    fun setUpTestData() {
        company =
            userRepository.save(
                User(
                    name = "토스",
                    email = "contact-${UUID.randomUUID()}@toss.im",
                    provider = OAuthProvider.SYSTEM,
                    identifier = "company-toss-${UUID.randomUUID()}",
                    role = UserRole.COMPANY,
                    profileImageUrl = "https://example.com/toss.png",
                ),
            )
    }

    @Test
    @DisplayName("v1 공개 링크 목록은 기본 정렬(PUBLISHED)로 정적 필드만 반환한다")
    fun getLinkContents_returnsStaticFieldsOnly() {
        val createdAt = Instant.parse("2026-04-25T10:15:30Z")
        val link = saveLink("Public Link", company, createdAtMillis = 1_000, createdAt = createdAt)

        given()
            .queryParam("size", 1)
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo(HttpStatus.OK.value()))
            .body("data.keySet()", containsInAnyOrder("content", "nextCursor", "hasNext", "size"))
            .body("data.content", hasSize<Any>(1))
            .body("data.content[0].id", equalTo(link.id.toString()))
            .body("data.content[0].sourceCompanyUserId", equalTo(company.id.toString()))
            .body("data.content[0].createdAt", equalTo("2026-04-25T10:15:30Z"))
            .body("data.hasNext", equalTo(false))
            .body("data.size", equalTo(1))
    }

    @Test
    @DisplayName("v1 PUBLISHED 정렬은 생성일 최신순으로 커서 페이지네이션한다")
    fun getLinkContents_published_paginatesByCursor() {
        val newest = saveLink("Newest", company, createdAtMillis = 2_000, createdAt = Instant.parse("2026-04-03T00:00:00Z"))
        val middle = saveLink("Middle", company, createdAtMillis = 1_000, createdAt = Instant.parse("2026-04-02T00:00:00Z"))
        val oldest = saveLink("Oldest", company, createdAtMillis = 3_000, createdAt = Instant.parse("2026-04-01T00:00:00Z"))

        val nextCursor =
            given()
                .queryParam("size", 2)
                .`when`()
                .get("/open-api/v1/links")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.content", hasSize<Any>(2))
                .body("data.content[0].id", equalTo(newest.id.toString()))
                .body("data.content[1].id", equalTo(middle.id.toString()))
                .body("data.hasNext", equalTo(true))
                .extract()
                .path<String>("data.nextCursor")

        given()
            .queryParam("cursor", nextCursor)
            .queryParam("size", 2)
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(1))
            .body("data.content[0].id", equalTo(oldest.id.toString()))
            .body("data.nextCursor", nullValue())
            .body("data.hasNext", equalTo(false))
    }

    @Test
    @DisplayName("v1 PUBLISHED 정렬은 period 기준 생성일 필터를 적용한다")
    fun getLinkContents_published_filtersByPeriod() {
        val recent =
            saveLink(
                "Recent",
                company,
                createdAtMillis = 1_000,
                createdAt = Instant.now().minus(1, ChronoUnit.DAYS),
            )
        saveLink(
            "Stale",
            company,
            createdAtMillis = 2_000,
            createdAt = Instant.now().minus(8, ChronoUnit.DAYS),
        )

        given()
            .queryParam("sort", "PUBLISHED")
            .queryParam("period", "WEEK")
            .queryParam("size", 10)
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(1))
            .body("data.content[0].id", equalTo(recent.id.toString()))
    }

    @Test
    @DisplayName("v1 LIKE 정렬은 기간 내 일별 좋아요 집계 합 기준으로 정렬한다")
    fun getLinkContents_like_ranksByPeriodDailyStats() {
        val mostLiked = saveLink("Most Liked", company, 1_000)
        val lessLiked = saveLink("Less Liked", company, 2_000)
        val notLikedInPeriod = saveLink("Stale", company, 3_000)
        createDailyStats(mostLiked, daysAgo = 0, likeCount = 5)
        createDailyStats(lessLiked, daysAgo = 2, likeCount = 2)
        createDailyStats(notLikedInPeriod, daysAgo = 40, likeCount = 99)

        given()
            .queryParam("sort", "LIKE")
            .queryParam("period", "MONTH")
            .queryParam("size", 10)
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(2))
            .body("data.content[0].id", equalTo(mostLiked.id.toString()))
            .body("data.content[1].id", equalTo(lessLiked.id.toString()))
    }

    @Test
    @DisplayName("v1 SAVE 정렬은 기간 내 일별 저장 집계 합 기준으로 정렬한다")
    fun getLinkContents_save_ranksByPeriodDailyStats() {
        val mostSaved = saveLink("Most Saved", company, 1_000)
        val lessSaved = saveLink("Less Saved", company, 2_000)
        createDailyStats(mostSaved, daysAgo = 1, saveCount = 7)
        createDailyStats(lessSaved, daysAgo = 2, saveCount = 3)

        given()
            .queryParam("sort", "SAVE")
            .queryParam("period", "MONTH")
            .queryParam("size", 10)
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(2))
            .body("data.content[0].id", equalTo(mostSaved.id.toString()))
            .body("data.content[1].id", equalTo(lessSaved.id.toString()))
    }

    @Test
    @DisplayName("v1 커서는 발급된 정렬과 다른 정렬로 요청하면 INVALID_LINK_CURSOR를 반환한다")
    fun getLinkContents_rejectsCursorFromDifferentSort() {
        saveLink("First", company, createdAtMillis = 1_000, createdAt = Instant.parse("2026-04-02T00:00:00Z"))
        saveLink("Second", company, createdAtMillis = 2_000, createdAt = Instant.parse("2026-04-01T00:00:00Z"))

        val createdAtCursor =
            given()
                .queryParam("size", 1)
                .`when`()
                .get("/open-api/v1/links")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.nextCursor", notNullValue())
                .extract()
                .path<String>("data.nextCursor")

        given()
            .queryParam("sort", "LIKE")
            .queryParam("cursor", createdAtCursor)
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("status", equalTo(6005))
            .body("message", equalTo("유효한 링크 커서가 아닙니다"))
    }

    @Test
    @DisplayName("v1 공개 링크 목록은 size 범위와 cursor 형식을 검증한다")
    fun getLinkContents_validatesSizeAndCursor() {
        given()
            .queryParam("size", 101)
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())

        given()
            .queryParam("cursor", "not-a-cursor")
            .`when`()
            .get("/open-api/v1/links")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("status", equalTo(6005))
            .body("message", equalTo("유효한 링크 커서가 아닙니다"))
    }

    private fun saveLink(
        title: String,
        sourceCompanyUser: User,
        createdAtMillis: Long,
        createdAt: Instant = Instant.ofEpochMilli(createdAtMillis),
    ): Link {
        return TransactionTemplate(transactionManager).execute {
            val managedSourceCompanyUser =
                userRepository.findById(
                    sourceCompanyUser.id ?: throw IllegalStateException("회사 사용자 ID가 없습니다"),
                ).orElseThrow()
            val link =
                linkRepository.save(
                    Link(
                        title = title,
                        url = "https://example.com/${UUID.randomUUID()}",
                        summary = "$title summary",
                        createdAt = createdAt,
                    ),
                )
            userLinkRepository.save(UserLink(user = managedSourceCompanyUser, link = link))
            link.createdAt = createdAt
            link.updatedAt = createdAt
            linkRepository.saveAndFlush(link)
        } ?: throw IllegalStateException("링크 저장에 실패했습니다")
    }

    private fun createDailyStats(
        link: Link,
        daysAgo: Long,
        likeCount: Long = 0,
        saveCount: Long = 0,
    ) {
        TransactionTemplate(transactionManager).execute {
            val managedLink = linkRepository.findById(link.id!!).orElseThrow()
            linkDailyStatsRepository.saveAndFlush(
                LinkDailyStats(
                    link = managedLink,
                    statDate = LocalDate.now(ZoneOffset.UTC).minusDays(daysAgo),
                    likeCount = likeCount,
                    saveCount = saveCount,
                ),
            )
        }
    }
}
