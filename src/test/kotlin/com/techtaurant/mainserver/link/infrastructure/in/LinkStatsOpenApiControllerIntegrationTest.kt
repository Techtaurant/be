package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID

@DisplayName("LinkStatsOpenApiController 통합 테스트")
class LinkStatsOpenApiControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var linkDailyStatsRepository: LinkDailyStatsRepository

    @BeforeEach
    fun setUpTestData() {
        linkDailyStatsRepository.deleteAllInBatch()
        linkRepository.deleteAllInBatch()
    }

    private fun saveLink(): Link =
        linkRepository.save(
            Link(
                title = "테스트 링크",
                url = "https://toss.tech/article/${UUID.randomUUID()}",
                summary = "테스트 링크 요약입니다.",
            ),
        )

    @Test
    @DisplayName("링크별 일별 통계를 전 기간 합산해 반환한다")
    fun getLinkStats_aggregatesDailyStatsAcrossDates() {
        // given
        val link = saveLink()
        linkDailyStatsRepository.save(
            LinkDailyStats(link, LocalDate.parse("2026-05-18"), viewCount = 10, likeCount = 2, saveCount = 1),
        )
        linkDailyStatsRepository.save(
            LinkDailyStats(link, LocalDate.parse("2026-05-19"), viewCount = 5, likeCount = 3, saveCount = 4),
        )

        // when & then
        given()
            .queryParam("linkIds", link.id)
            .`when`()
            .get("/open-api/links/stats")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.find { it.linkId == '${link.id}' }.viewCount", equalTo(15))
            .body("data.find { it.linkId == '${link.id}' }.likeCount", equalTo(5))
            .body("data.find { it.linkId == '${link.id}' }.saveCount", equalTo(5))
    }

    @Test
    @DisplayName("통계 이력이 없는 링크는 응답에서 제외된다")
    fun getLinkStats_excludesLinksWithoutStats() {
        // given
        val linkWithStats = saveLink()
        val linkWithoutStats = saveLink()
        linkDailyStatsRepository.save(
            LinkDailyStats(linkWithStats, LocalDate.parse("2026-05-18"), viewCount = 1, likeCount = 0, saveCount = 0),
        )

        // when & then
        given()
            .queryParam("linkIds", linkWithStats.id, linkWithoutStats.id)
            .`when`()
            .get("/open-api/links/stats")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.linkId", hasItem(linkWithStats.id.toString()))
            .body("data.linkId", not(hasItem(linkWithoutStats.id.toString())))
    }

    @Test
    @DisplayName("linkIds가 100개를 초과하면 400을 반환한다")
    fun getLinkStats_whenTooManyLinkIds_returnsBadRequest() {
        // given
        val tooManyLinkIds = (1..101).map { UUID.randomUUID() }

        // when & then
        given()
            .queryParam("linkIds", tooManyLinkIds)
            .`when`()
            .get("/open-api/links/stats")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }
}
