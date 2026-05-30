package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkReadLog
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

@DisplayName("LinkViewerStateController 통합 테스트")
class LinkViewerStateControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var userLinkRepository: UserLinkRepository

    @Autowired
    private lateinit var linkReadLogRepository: LinkReadLogRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var companyUser: User
    private lateinit var normalUser: User
    private lateinit var accessToken: String
    private lateinit var savedLink: Link
    private lateinit var readLink: Link

    @BeforeEach
    fun setUpTestData() {
        companyUser = createUser("회사", UserRole.COMPANY)
        normalUser = createUser("일반사용자", UserRole.USER)
        accessToken = jwtTokenProvider.createAccessToken(normalUser.id!!, normalUser.role)

        savedLink = createLink("저장한 링크")
        readLink = createLink("읽은 링크")
    }

    @Test
    @DisplayName("비로그인 사용자는 링크 사용자 상태를 조회할 수 없다")
    fun getLinkViewerStates_anonymous_returnsUnauthorized() {
        // given
        val link = savedLink

        // when & then
        given()
            .queryParam("linkIds", link.id)
            .`when`()
            .get("/api/links/me/states")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("로그인 사용자는 링크별 저장과 읽음 상태를 조회한다")
    fun getLinkViewerStates_authenticated_returnsViewerStates() {
        // given
        userLinkRepository.save(UserLink(user = normalUser, link = savedLink))
        linkReadLogRepository.save(LinkReadLog(user = normalUser, link = readLink))

        // when & then
        given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("linkIds", readLink.id, savedLink.id)
            .`when`()
            .get("/api/links/me/states")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data[0].linkId", equalTo(readLink.id.toString()))
            .body("data[0].isSaved", equalTo(false))
            .body("data[0].isRead", equalTo(true))
            .body("data[1].linkId", equalTo(savedLink.id.toString()))
            .body("data[1].isSaved", equalTo(true))
            .body("data[1].isRead", equalTo(false))
    }

    private fun createUser(
        name: String,
        role: UserRole,
    ): User =
        userRepository.save(
            User(
                name = name,
                email = "${UUID.randomUUID()}@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = UUID.randomUUID().toString(),
                role = role,
                profileImageUrl = "https://example.com/profile.jpg",
            ),
        )

    private fun createLink(title: String): Link =
        linkRepository.save(
            Link(
                title = title,
                url = "https://example.com/${UUID.randomUUID()}",
                summary = "요약",
            ),
        )
}
