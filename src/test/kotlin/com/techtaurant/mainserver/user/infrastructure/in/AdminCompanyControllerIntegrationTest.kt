package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import com.techtaurant.mainserver.link.entity.LinkReadLog
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("AdminCompanyController 통합 테스트")
class AdminCompanyControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var linkCrawlBatchRepository: LinkCrawlBatchRepository

    @Autowired
    private lateinit var userLinkRepository: UserLinkRepository

    @Autowired
    private lateinit var linkReadLogRepository: LinkReadLogRepository

    @Autowired
    private lateinit var tagRepository: TagRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminUser: User
    private lateinit var normalUser: User
    private lateinit var adminAccessToken: String
    private lateinit var userAccessToken: String

    @BeforeEach
    fun setUpTestData() {
        userRepository.deleteAllInBatch()

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

        normalUser =
            userRepository.save(
                User(
                    name = "일반사용자",
                    email = "user-${UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "user-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/user-profile.jpg",
                ),
            )

        adminAccessToken = jwtTokenProvider.createAccessToken(adminUser.id!!, adminUser.role)
        userAccessToken = jwtTokenProvider.createAccessToken(normalUser.id!!, normalUser.role)
    }

    @Test
    @DisplayName("ADMIN 권한은 회사를 COMPANY 사용자로 등록할 수 있다")
    fun adminCanCreateCompanyUser() {
        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $adminAccessToken")
            .body(
                """
                {
                  "name": "토스",
                  "email": "contact@toss.im",
                  "profileImageUrl": "https://static.toss.im/logo.png"
                }
                """.trimIndent(),
            ).`when`()
            .post("/admin/companies")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.name", equalTo("토스"))
            .body("data.email", equalTo("contact@toss.im"))
            .body("data.profileImageUrl", equalTo("https://static.toss.im/logo.png"))

        val savedCompany =
            userRepository.findAll().first { it.name == "토스" }

        assertEquals(UserRole.COMPANY, savedCompany.role)
        assertEquals(OAuthProvider.SYSTEM, savedCompany.provider)
    }

    @Test
    @DisplayName("USER 권한은 회사 등록 API를 호출할 수 없다")
    fun userCannotCreateCompanyUser() {
        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $userAccessToken")
            .body(
                """
                {
                  "name": "토스",
                  "email": "contact@toss.im"
                }
                """.trimIndent(),
            ).`when`()
            .post("/admin/companies")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("ADMIN 권한은 등록된 회사 목록만 조회할 수 있다")
    fun adminCanGetCompanies() {
        userRepository.save(
            User(
                name = "당근",
                email = "hello@daangn.com",
                provider = OAuthProvider.SYSTEM,
                identifier = "company-daangn",
                role = UserRole.COMPANY,
                profileImageUrl = "https://example.com/daangn.png",
            ),
        )
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

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .get("/admin/companies")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(2))
            .body("data[0].name", equalTo("당근"))
            .body("data[1].name", equalTo("토스"))
    }

    @Test
    @DisplayName("ADMIN 권한은 회사와 관련 배치 및 링크를 삭제할 수 있다")
    fun adminCanDeleteCompanyWithBatchesAndLinks() {
        val company = saveCompanyUser("토스")
        val otherCompany = saveCompanyUser("당근")
        val linkTag = tagRepository.save(Tag(name = "backend"))
        val companyLink = saveLink(company, "토스 링크", "https://toss.tech/article/delete-target")
        val otherCompanyLink = saveLink(otherCompany, "당근 링크", "https://medium.com/daangn/delete-survivor")

        saveBatch(company, "토스 배치")
        saveBatch(otherCompany, "당근 배치")
        jdbcTemplate.update("INSERT INTO link_tags(link_id, tag_id) VALUES (?, ?)", companyLink.id, linkTag.id)
        userLinkRepository.save(UserLink(user = normalUser, link = companyLink))
        linkReadLogRepository.save(LinkReadLog(user = normalUser, link = companyLink))

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .delete("/admin/companies/${company.id}")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        val companyId = company.id!!
        val companyLinkId = companyLink.id!!

        assertFalse(userRepository.existsById(companyId))
        assertTrue(userRepository.existsById(otherCompany.id!!))
        assertTrue(linkRepository.existsById(otherCompanyLink.id!!))
        assertFalse(linkRepository.existsById(companyLinkId))
        assertTrue(linkCrawlBatchRepository.findAllByCompanyUserId(companyId).isEmpty())
        assertEquals(1, linkCrawlBatchRepository.findAllByCompanyUserId(otherCompany.id!!).size)
        assertEquals(0, countLinkTags(companyLinkId))
        assertTrue(tagRepository.existsById(linkTag.id!!))
        assertNull(userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, companyLinkId))
        assertFalse(linkReadLogRepository.existsByUserIdAndLinkId(normalUser.id!!, companyLinkId))
    }

    @Test
    @DisplayName("ADMIN 권한이라도 COMPANY 역할이 아닌 사용자는 회사 삭제 대상으로 처리하지 않는다")
    fun adminCannotDeleteNonCompanyUserAsCompany() {
        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .delete("/admin/companies/${normalUser.id}")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())

        assertTrue(userRepository.existsById(normalUser.id!!))
    }

    private fun saveCompanyUser(name: String): User {
        return userRepository.save(
            User(
                name = name,
                email = "${UUID.randomUUID()}@example.com",
                provider = OAuthProvider.SYSTEM,
                identifier = "company-${UUID.randomUUID()}",
                role = UserRole.COMPANY,
                profileImageUrl = "https://example.com/company.png",
            ),
        )
    }

    private fun saveBatch(
        company: User,
        name: String,
    ): LinkCrawlBatch {
        return linkCrawlBatchRepository.save(
            LinkCrawlBatch(
                companyUser = company,
                name = name,
                baseUrl = "https://example.com",
                pageUriTemplate = "/articles?page={page}",
                itemSelector = ".article-card",
                articleLinkSelector = "a.article-link",
                titleSelector = ".title",
                summarySelector = ".summary",
                publishedAtSelectors = "time",
                tagNames = "backend",
                cronExpression = "0 0 * * * *",
                startPage = 1,
                active = true,
            ),
        )
    }

    private fun saveLink(
        company: User,
        title: String,
        url: String,
    ): Link {
        val link =
            linkRepository.save(
                Link(
                    title = title,
                    url = url,
                    summary = "링크 요약",
                ),
            )
        userLinkRepository.save(UserLink(user = company, link = link))
        return link
    }

    private fun countLinkTags(linkId: UUID): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM link_tags WHERE link_id = ?",
            Int::class.java,
            linkId,
        ) ?: 0
    }
}
