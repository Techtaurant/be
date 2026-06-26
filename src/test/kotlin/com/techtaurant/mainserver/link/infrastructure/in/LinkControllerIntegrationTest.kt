package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

@DisplayName("LinkController 통합 테스트")
class LinkControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var linkRepository: com.techtaurant.mainserver.link.infrastructure.out.LinkRepository

    @Autowired
    private lateinit var userLinkRepository: com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var companyUser: User
    private lateinit var normalUser: User
    private lateinit var anotherUser: User
    private lateinit var accessToken: String
    private lateinit var anotherAccessToken: String
    private lateinit var firstLink: Link
    private lateinit var secondLink: Link

    @BeforeEach
    fun setUpTestData() {
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

        normalUser =
            userRepository.save(
                User(
                    name = "일반사용자",
                    email = "user-${UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "user-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/user.png",
                ),
            )

        anotherUser =
            userRepository.save(
                User(
                    name = "다른사용자",
                    email = "another-${UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "another-user-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/another.png",
                ),
            )

        accessToken = jwtTokenProvider.createAccessToken(normalUser.id!!, normalUser.role)
        anotherAccessToken = jwtTokenProvider.createAccessToken(anotherUser.id!!, anotherUser.role)

        firstLink =
            linkRepository.save(
                Link(
                    title = "Metric Review, 실행을 이끌다",
                    url = "https://toss.tech/article/metric-review",
                    summary = "지표 리뷰를 실행으로 연결한 사례입니다.",
                ),
            )
        userLinkRepository.save(UserLink(user = companyUser, link = firstLink))

        secondLink =
            linkRepository.save(
                Link(
                    title = "StarRocks 운영기",
                    url = "https://toss.tech/article/starrocks",
                    summary = "멀티테넌트 워크로드 격리 전략을 소개합니다.",
                ),
            )
        userLinkRepository.save(UserLink(user = companyUser, link = secondLink))
    }

    @Test
    @DisplayName("사용자는 회사 링크 목록을 조회하고 저장/읽음 상태를 확인할 수 있다")
    fun userCanListCompanyLinksWithSavedAndReadFlags() {
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/companies/${companyUser.id}/links?size=10")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(2))
            .body("data.content.find { it.id == '${firstLink.id}' }.sourceCompanyUserId", equalTo(companyUser.id.toString()))
            .body("data.content.find { it.id == '${firstLink.id}' }.isSaved", equalTo(false))
            .body("data.content.find { it.id == '${firstLink.id}' }.isRead", equalTo(false))
            .body("data.content.find { it.id == '${firstLink.id}' }.viewCount", equalTo(0))
            .body("data.content.find { it.id == '${firstLink.id}' }.likeCount", equalTo(0))

        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/links/${firstLink.id}/save")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $accessToken")
            .body("""{"isRead": true}""")
            .`when`()
            .post("/api/links/${firstLink.id}/read-logs")
            .then()
            .statusCode(HttpStatus.OK.value())

        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/companies/${companyUser.id}/links?size=10")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.find { it.id == '${firstLink.id}' }.isSaved", equalTo(true))
            .body("data.content.find { it.id == '${firstLink.id}' }.isRead", equalTo(true))
    }

    @Test
    @DisplayName("사용자는 링크 저장을 취소하고 읽음 상태를 해제할 수 있다")
    fun userCanUnsaveAndUnreadLink() {
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/links/${secondLink.id}/save")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $accessToken")
            .body("""{"isRead": true}""")
            .`when`()
            .post("/api/links/${secondLink.id}/read-logs")
            .then()
            .statusCode(HttpStatus.OK.value())

        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/links/${secondLink.id}/save")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $accessToken")
            .body("""{"isRead": false}""")
            .`when`()
            .post("/api/links/${secondLink.id}/read-logs")
            .then()
            .statusCode(HttpStatus.OK.value())

        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/companies/${companyUser.id}/links?size=10")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.find { it.id == '${secondLink.id}' }.isSaved", equalTo(false))
            .body("data.content.find { it.id == '${secondLink.id}' }.isRead", equalTo(false))
    }

    @Test
    @DisplayName("사용자는 링크 좋아요를 기록하고 조회 로그는 링크 조회수를 증가시킨다")
    fun userCanRecordLikeAndViewCountForLink() {
        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $accessToken")
            .body("""{"likeStatus": "LIKE"}""")
            .`when`()
            .post("/api/links/${firstLink.id}/like")
            .then()
            .statusCode(HttpStatus.OK.value())

        given()
            .header("User-Agent", "RestAssured")
            .`when`()
            .post("/open-api/links/${firstLink.id}/view-logs")
            .then()
            .statusCode(HttpStatus.OK.value())

        given()
            .header("User-Agent", "RestAssured")
            .`when`()
            .post("/open-api/links/${firstLink.id}/view-logs")
            .then()
            .statusCode(HttpStatus.OK.value())

        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/companies/${companyUser.id}/links?size=10")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.find { it.id == '${firstLink.id}' }.likeCount", equalTo(1))
            .body("data.content.find { it.id == '${firstLink.id}' }.viewCount", equalTo(2))
    }

    @Test
    @DisplayName("사용자는 링크를 직접 등록할 수 있다")
    fun userCanCreateLink() {
        val createdLinkId =
            given()
                .contentType("application/json")
                .header("Authorization", "Bearer $accessToken")
                .body(
                    """
                    {
                      "title": "<b>직접 등록한 링크</b>",
                      "url": "https://example.com/articles/direct-link",
                      "summary": "직접 등록한 링크 요약입니다.",
                      "tags": ["Kotlin", "api"]
                    }
                    """.trimIndent(),
                )
                .`when`()
                .post("/api/links")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("data.title", equalTo("직접 등록한 링크"))
                .body("data.url", equalTo("https://example.com/articles/direct-link"))
                .body("data.summary", equalTo("직접 등록한 링크 요약입니다."))
                .body("data.sourceCompanyUserId", equalTo(normalUser.id.toString()))
                .body("data.tags", containsInAnyOrder("api", "kotlin"))
                .extract()
                .path<String>("data.id")

        val createdLink = linkRepository.findByUrl("https://example.com/articles/direct-link")
        assertNotNull(createdLink)
        assertEquals(createdLinkId, createdLink!!.id.toString())
        assertNotNull(userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, createdLink.id!!))
    }

    @Test
    @DisplayName("첫 번째 등록자는 링크를 수정할 수 있다")
    fun firstSourceCanUpdateLink() {
        val createdLinkId = createLinkByApi(accessToken, "https://example.com/articles/updatable-link")

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $accessToken")
            .body(
                """
                {
                  "title": "수정된 링크 제목",
                  "summary": "수정된 링크 요약입니다.",
                  "tags": ["Spring", "backend"]
                }
                """.trimIndent(),
            )
            .`when`()
            .patch("/api/links/$createdLinkId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(createdLinkId))
            .body("data.title", equalTo("수정된 링크 제목"))
            .body("data.summary", equalTo("수정된 링크 요약입니다."))
            .body("data.tags", containsInAnyOrder("backend", "spring"))

        val updatedLink = linkRepository.findByIdWithTags(UUID.fromString(createdLinkId))
        assertNotNull(updatedLink)
        assertEquals("수정된 링크 제목", updatedLink!!.title)
        assertEquals("수정된 링크 요약입니다.", updatedLink.summary)
        assertEquals(setOf("backend", "spring"), updatedLink.tags.map { it.name }.toSet())
    }

    @Test
    @DisplayName("첫 번째 등록자가 아닌 사용자는 링크를 수정할 수 없다")
    fun nonFirstSourceCannotUpdateLink() {
        val createdLinkId = createLinkByApi(accessToken, "https://example.com/articles/owned-by-first-user")

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $anotherAccessToken")
            .body(
                """
                {
                  "title": "권한 없는 수정",
                  "summary": "수정되면 안 됩니다."
                }
                """.trimIndent(),
            )
            .`when`()
            .patch("/api/links/$createdLinkId")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("status", equalTo(6008))
    }

    private fun createLinkByApi(
        token: String,
        url: String,
    ): String {
        return given()
            .contentType("application/json")
            .header("Authorization", "Bearer $token")
            .body(
                """
                {
                  "title": "등록할 링크",
                  "url": "$url",
                  "summary": "등록할 링크 요약입니다.",
                  "tags": ["tech"]
                }
                """.trimIndent(),
            )
            .`when`()
            .post("/api/links")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("data.id")
    }
}
