package com.techtaurant.mainserver.link.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
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
import java.util.UUID

@DisplayName("LinkReadOpenApiController 통합 테스트")
class LinkReadOpenApiControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var userLinkRepository: UserLinkRepository

    @Autowired
    private lateinit var tagRepository: TagRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    private lateinit var firstCompany: User
    private lateinit var secondCompany: User

    @BeforeEach
    fun setUpTestData() {
        firstCompany =
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

        secondCompany =
            userRepository.save(
                User(
                    name = "당근",
                    email = "contact-${UUID.randomUUID()}@daangn.com",
                    provider = OAuthProvider.SYSTEM,
                    identifier = "company-daangn-${UUID.randomUUID()}",
                    role = UserRole.COMPANY,
                    profileImageUrl = "https://example.com/daangn.png",
                ),
            )
    }

    @Test
    @DisplayName("공개 링크 목록은 정적 링크 필드만 ApiResponse와 CursorPageResponse 형태로 반환한다")
    fun getLinkContents_returnsStaticFieldsOnly() {
        val linkTag = tagRepository.save(Tag(name = "Spring"))
        val anotherLinkTag = tagRepository.save(Tag(name = "Kotlin"))
        val publishedAt = Instant.parse("2026-04-25T10:15:30Z")
        val link =
            saveLink(
                title = "Public Link",
                url = "https://example.com/public-link",
                sourceCompanyUser = firstCompany,
                createdAtMillis = 1_000,
                publishedAt = publishedAt,
                tags = mutableSetOf(linkTag, anotherLinkTag),
            )
        userLinkRepository.saveAndFlush(UserLink(user = secondCompany, link = link, isSource = true))

        given()
            .queryParam("size", 1)
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo(HttpStatus.OK.value()))
            .body("message", equalTo("OK"))
            .body("data.keySet()", containsInAnyOrder("content", "nextCursor", "hasNext", "size"))
            .body("data.content", hasSize<Any>(1))
            .body(
                "data.content[0].keySet()",
                containsInAnyOrder(
                    "id",
                    "title",
                    "url",
                    "summary",
                    "sourceCompanyUserId",
                    "publishedAt",
                    "tags",
                    "createdAt",
                    "updatedAt",
                ),
            )
            .body("data.content[0].id", equalTo(link.id.toString()))
            .body("data.content[0].title", equalTo("Public Link"))
            .body("data.content[0].url", equalTo("https://example.com/public-link"))
            .body("data.content[0].summary", equalTo("Public Link summary"))
            .body("data.content[0].sourceCompanyUserId", equalTo(firstCompany.id.toString()))
            .body("data.content[0].publishedAt", equalTo("2026-04-25T10:15:30Z"))
            .body("data.content[0].tags", containsInAnyOrder("Kotlin", "Spring"))
            .body("data.content[0].createdAt", notNullValue())
            .body("data.content[0].updatedAt", notNullValue())
            .body("data.content[0].isSaved", nullValue())
            .body("data.content[0].isRead", nullValue())
            .body("data.nextCursor", nullValue())
            .body("data.hasNext", equalTo(false))
            .body("data.size", equalTo(1))
    }

    @Test
    @DisplayName("공개 링크 목록은 인증 없이 cursor와 size로 최신순 페이지네이션한다")
    fun getLinkContents_paginatesByCursorWithoutAuthentication() {
        val oldest = saveLink("Oldest", "https://example.com/oldest", firstCompany, 1_000)
        val middle = saveLink("Middle", "https://example.com/middle", firstCompany, 2_000)
        val newest = saveLink("Newest", "https://example.com/newest", firstCompany, 3_000)

        val nextCursor =
            given()
                .queryParam("size", 2)
                .`when`()
                .get("/open-api/links")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.content", hasSize<Any>(2))
                .body("data.content[0].id", equalTo(newest.id.toString()))
                .body("data.content[1].id", equalTo(middle.id.toString()))
                .body("data.nextCursor", notNullValue())
                .body("data.hasNext", equalTo(true))
                .body("data.size", equalTo(2))
                .extract()
                .path<String>("data.nextCursor")

        given()
            .queryParam("cursor", nextCursor)
            .queryParam("size", 2)
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(1))
            .body("data.content[0].id", equalTo(oldest.id.toString()))
            .body("data.nextCursor", nullValue())
            .body("data.hasNext", equalTo(false))
            .body("data.size", equalTo(1))
    }

    @Test
    @DisplayName("공개 링크 목록은 size 기본값 20을 사용한다")
    fun getLinkContents_usesDefaultSizeTwenty() {
        repeat(21) { index ->
            saveLink(
                title = "Link $index",
                url = "https://example.com/default-size-$index",
                sourceCompanyUser = firstCompany,
                createdAtMillis = index.toLong(),
            )
        }

        given()
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(20))
            .body("data.hasNext", equalTo(true))
            .body("data.size", equalTo(20))
    }

    @Test
    @DisplayName("공개 링크 목록은 sourceCompanyUserId와 tag로 필터링한다")
    fun getLinkContents_filtersBySourceCompanyUserIdAndTag() {
        val springTag = tagRepository.save(Tag(name = "Spring"))
        val kotlinTag = tagRepository.save(Tag(name = "Kotlin"))
        val firstCompanySpringLink =
            saveLink(
                title = "First Company Spring",
                url = "https://example.com/first-company-spring",
                sourceCompanyUser = firstCompany,
                createdAtMillis = 1_000,
                tags = mutableSetOf(springTag),
            )
        val firstCompanyKotlinLink =
            saveLink(
                title = "First Company Kotlin",
                url = "https://example.com/first-company-kotlin",
                sourceCompanyUser = firstCompany,
                createdAtMillis = 2_000,
                tags = mutableSetOf(kotlinTag),
            )
        val secondCompanySpringLink =
            saveLink(
                title = "Second Company Spring",
                url = "https://example.com/second-company-spring",
                sourceCompanyUser = secondCompany,
                createdAtMillis = 3_000,
                tags = mutableSetOf(springTag),
            )

        given()
            .queryParam("sourceCompanyUserId", firstCompany.id.toString())
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(2))
            .body("data.content.id", hasItem(firstCompanySpringLink.id.toString()))
            .body("data.content.id", hasItem(firstCompanyKotlinLink.id.toString()))
            .body("data.content.id", not(hasItem(secondCompanySpringLink.id.toString())))

        given()
            .queryParam("tag", "Spring")
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(2))
            .body("data.content.id", hasItem(firstCompanySpringLink.id.toString()))
            .body("data.content.id", hasItem(secondCompanySpringLink.id.toString()))
            .body("data.content.id", not(hasItem(firstCompanyKotlinLink.id.toString())))
    }

    @Test
    @DisplayName("공개 회사 링크 목록은 인증 없이 회사별 링크를 커서 기반으로 조회한다")
    fun getCompanyLinkContents_paginatesCompanyLinksWithoutAuthentication() {
        val oldest = saveLink("Oldest Company Link", "https://example.com/company-oldest", firstCompany, 1_000)
        val middle = saveLink("Middle Company Link", "https://example.com/company-middle", firstCompany, 2_000)
        val newest = saveLink("Newest Company Link", "https://example.com/company-newest", firstCompany, 3_000)
        val otherCompanyLink = saveLink("Other Company Link", "https://example.com/company-other", secondCompany, 4_000)

        val nextCursor =
            given()
                .queryParam("size", 2)
                .`when`()
                .get("/open-api/companies/${firstCompany.id}/links")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.content", hasSize<Any>(2))
                .body("data.content[0].id", equalTo(newest.id.toString()))
                .body("data.content[1].id", equalTo(middle.id.toString()))
                .body("data.content.id", not(hasItem(otherCompanyLink.id.toString())))
                .body("data.nextCursor", notNullValue())
                .body("data.hasNext", equalTo(true))
                .body("data.size", equalTo(2))
                .extract()
                .path<String>("data.nextCursor")

        given()
            .queryParam("cursor", nextCursor)
            .queryParam("size", 2)
            .`when`()
            .get("/open-api/companies/${firstCompany.id}/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(1))
            .body("data.content[0].id", equalTo(oldest.id.toString()))
            .body("data.nextCursor", nullValue())
            .body("data.hasNext", equalTo(false))
            .body("data.size", equalTo(1))
    }

    @Test
    @DisplayName("공개 회사 링크 목록은 없는 회사 ID면 COMPANY_NOT_FOUND를 반환한다")
    fun getCompanyLinkContents_missingCompany_returnsCompanyNotFound() {
        given()
            .`when`()
            .get("/open-api/companies/${UUID.randomUUID()}/links")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("status", equalTo(1010))
            .body("message", equalTo("회사를 찾을 수 없습니다"))
    }

    @Test
    @DisplayName("공개 링크 목록은 결과가 없으면 빈 커서 페이지를 반환한다")
    fun getLinkContents_returnsEmptyPageForNoMatches() {
        saveLink("Valid Link", "https://example.com/valid-link", firstCompany, 1_000)

        given()
            .queryParam("tag", "Unknown")
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", hasSize<Any>(0))
            .body("data.nextCursor", nullValue())
            .body("data.hasNext", equalTo(false))
            .body("data.size", equalTo(0))
    }

    @Test
    @DisplayName("공개 링크 목록은 size 범위와 cursor 형식을 검증한다")
    fun getLinkContents_validatesSizeAndCursor() {
        given()
            .queryParam("size", 0)
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())

        given()
            .queryParam("size", 101)
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())

        given()
            .queryParam("cursor", "not-a-cursor")
            .`when`()
            .get("/open-api/links")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("status", equalTo(6005))
            .body("message", equalTo("유효한 링크 커서가 아닙니다"))
    }

    @Test
    @DisplayName("공개 링크 상세 조회는 정적 링크 필드를 반환한다")
    fun getLinkContentDetail_returnsStaticFieldsOnly() {
        val linkTag = tagRepository.save(Tag(name = "Architecture"))
        val anotherLinkTag = tagRepository.save(Tag(name = "Kotlin"))
        val publishedAt = Instant.parse("2026-04-26T11:20:30Z")
        val link =
            saveLink(
                title = "Detail Link",
                url = "https://example.com/detail-link",
                sourceCompanyUser = firstCompany,
                createdAtMillis = 4_000,
                publishedAt = publishedAt,
                tags = mutableSetOf(linkTag, anotherLinkTag),
            )
        userLinkRepository.saveAndFlush(UserLink(user = secondCompany, link = link, isSource = true))

        given()
            .`when`()
            .get("/open-api/links/${link.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo(HttpStatus.OK.value()))
            .body(
                "data.keySet()",
                containsInAnyOrder(
                    "id",
                    "title",
                    "url",
                    "summary",
                    "sourceCompanyUserId",
                    "publishedAt",
                    "tags",
                    "createdAt",
                    "updatedAt",
                ),
            )
            .body("data.id", equalTo(link.id.toString()))
            .body("data.title", equalTo("Detail Link"))
            .body("data.url", equalTo("https://example.com/detail-link"))
            .body("data.summary", equalTo("Detail Link summary"))
            .body("data.sourceCompanyUserId", equalTo(firstCompany.id.toString()))
            .body("data.publishedAt", equalTo("2026-04-26T11:20:30Z"))
            .body("data.tags", containsInAnyOrder("Architecture", "Kotlin"))
            .body("data.createdAt", notNullValue())
            .body("data.updatedAt", notNullValue())
            .body("data.isSaved", nullValue())
            .body("data.isRead", nullValue())
    }

    @Test
    @DisplayName("공개 링크 상세 조회는 없는 링크 ID면 LINK_NOT_FOUND를 반환한다")
    fun getLinkContentDetail_missingLink_returnsLinkNotFound() {
        given()
            .`when`()
            .get("/open-api/links/${UUID.randomUUID()}")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("status", equalTo(6001))
            .body("data", nullValue())
            .body("message", equalTo("링크를 찾을 수 없습니다"))
    }

    private fun saveLink(
        title: String,
        url: String,
        sourceCompanyUser: User,
        createdAtMillis: Long,
        publishedAt: Instant? = null,
        tags: MutableSet<Tag> = mutableSetOf(),
    ): Link {
        return TransactionTemplate(transactionManager).execute {
            val managedSourceCompanyUser =
                userRepository.findById(
                    sourceCompanyUser.id ?: throw IllegalStateException("회사 사용자 ID가 없습니다"),
                ).orElseThrow()
            val managedTags =
                tags.map { tag ->
                    tagRepository.findById(tag.id ?: throw IllegalStateException("태그 ID가 없습니다")).orElseThrow()
                }.toMutableSet()
            val link =
                linkRepository.save(
                    Link(
                        title = title,
                        url = url,
                        summary = "$title summary",
                        publishedAt = publishedAt,
                        tags = managedTags,
                    ),
                )
            userLinkRepository.save(UserLink(user = managedSourceCompanyUser, link = link, isSource = true))
            link.createdAt = Instant.ofEpochMilli(createdAtMillis)
            link.updatedAt = Instant.ofEpochMilli(createdAtMillis)
            linkRepository.saveAndFlush(link)
        } ?: throw IllegalStateException("링크 저장에 실패했습니다")
    }
}
