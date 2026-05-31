package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.attachment.application.S3StorageService
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import com.techtaurant.mainserver.attachment.infrastructure.out.AttachmentRepository
import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.infrastructure.out.CommentLikeLogRepository
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkCrawlBatch
import com.techtaurant.mainserver.link.entity.LinkReadLog
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkCrawlBatchRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkLikeLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostDailyStats
import com.techtaurant.mainserver.post.entity.Tag
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserTokenRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.Base64
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("AdminCompanyController 통합 테스트")
class AdminCompanyControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var linkDailyStatsRepository: LinkDailyStatsRepository

    @Autowired
    private lateinit var linkLikeLogRepository: LinkLikeLogRepository

    @Autowired
    private lateinit var linkCrawlBatchRepository: LinkCrawlBatchRepository

    @Autowired
    private lateinit var userLinkRepository: UserLinkRepository

    @Autowired
    private lateinit var linkReadLogRepository: LinkReadLogRepository

    @Autowired
    private lateinit var tagRepository: TagRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postDailyStatsRepository: PostDailyStatsRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var commentLikeLogRepository: CommentLikeLogRepository

    @Autowired
    private lateinit var postLikeLogRepository: PostLikeLogRepository

    @Autowired
    private lateinit var attachmentRepository: AttachmentRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var userTokenRepository: UserTokenRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var s3StorageService: S3StorageService

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
        val sharedLink = saveLink(company, "공유 링크", "https://example.com/shared-link")

        saveBatch(company, "토스 배치")
        saveBatch(otherCompany, "당근 배치")
        jdbcTemplate.update("INSERT INTO link_tags(link_id, tag_id) VALUES (?, ?)", companyLink.id, linkTag.id)
        userLinkRepository.save(UserLink(user = normalUser, link = sharedLink))
        userLinkRepository.save(UserLink(user = otherCompany, link = companyLink))
        userLinkRepository.save(UserLink(user = otherCompany, link = sharedLink, isSource = true))
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
        val sharedLinkId = sharedLink.id!!

        assertFalse(userRepository.existsById(companyId))
        assertTrue(userRepository.existsById(otherCompany.id!!))
        assertTrue(linkRepository.existsById(otherCompanyLink.id!!))
        assertFalse(linkRepository.existsById(companyLinkId))
        assertTrue(linkRepository.existsById(sharedLinkId))
        assertTrue(linkCrawlBatchRepository.findAllByCompanyUserId(companyId).isEmpty())
        assertEquals(1, linkCrawlBatchRepository.findAllByCompanyUserId(otherCompany.id!!).size)
        assertEquals(0, countLinkTags(companyLinkId))
        assertTrue(tagRepository.existsById(linkTag.id!!))
        assertNull(userLinkRepository.findSavedByUserIdAndLinkId(normalUser.id!!, companyLinkId))
        assertNull(userLinkRepository.findSourceByUserIdAndLinkId(companyId, sharedLinkId))
        assertTrue(userLinkRepository.findSourceByUserIdAndLinkId(otherCompany.id!!, sharedLinkId) != null)
        assertFalse(linkReadLogRepository.existsByUserIdAndLinkId(normalUser.id!!, companyLinkId))

        given()
            .`when`()
            .get("/open-api/links/$sharedLinkId")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.sourceCompanyUserId", equalTo(otherCompany.id.toString()))
    }

    @Test
    @DisplayName("ADMIN 권한은 회사 작성 게시물의 첨부파일을 함께 삭제할 수 있다")
    fun adminCanDeleteCompanyWithAuthoredPostAttachments() {
        val company = saveCompanyUser("토스")
        val post =
            postRepository.saveAndFlush(
                Post(
                    title = "회사 작성글",
                    content = "회사 작성글 본문",
                    author = company,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        val attachment =
            attachmentRepository.saveAndFlush(
                Attachment(
                    referenceId = post.id,
                    referenceType = AttachmentReferenceType.POST,
                    objectKey = "posts/${post.id}/image.png",
                    status = AttachmentStatus.CONFIRMED,
                    originalFileName = "image.png",
                    contentType = "image/png",
                    fileSize = 100,
                ),
            )
        post.thumbnailImage = attachment.id
        postRepository.saveAndFlush(post)

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .delete("/admin/companies/${company.id}")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        val postId = post.id!!
        assertFalse(postRepository.existsById(postId))
        assertTrue(
            attachmentRepository
                .findAllByReferenceIdAndReferenceType(postId, AttachmentReferenceType.POST)
                .isEmpty(),
        )
    }

    @Test
    @DisplayName("ADMIN 권한은 회사를 삭제할 때 남아 있는 저장 링크의 일별 저장수를 차감한다")
    fun adminCanDeleteCompanyAndDecrementSavedLinkStats() {
        val company = saveCompanyUser("토스")
        val sourceCompany = saveCompanyUser("당근")
        val link = saveLink(sourceCompany, "당근 링크", "https://example.com/surviving-link")
        val companyAccessToken = jwtTokenProvider.createAccessToken(company.id!!, company.role)

        given()
            .header("Authorization", "Bearer $companyAccessToken")
            .`when`()
            .post("/api/links/${link.id}/save")
            .then()
            .statusCode(HttpStatus.CREATED.value())

        assertEquals(1, linkDailyStatsRepository.findAll().single().saveCount)

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .delete("/admin/companies/${company.id}")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        assertTrue(linkRepository.existsById(link.id!!))
        assertEquals(0, linkDailyStatsRepository.findAll().single().saveCount)
        assertNull(userLinkRepository.findSavedByUserIdAndLinkId(company.id!!, link.id!!))
    }

    @Test
    @DisplayName("ADMIN 권한은 회사를 삭제할 때 남아 있는 링크의 좋아요 통계를 되돌린다")
    fun adminCanDeleteCompanyAndAdjustSurvivingLinkLikeStats() {
        val company = saveCompanyUser("토스")
        val sourceCompany = saveCompanyUser("당근")
        val link = saveLink(sourceCompany, "당근 링크", "https://example.com/surviving-liked-link")
        val companyAccessToken = jwtTokenProvider.createAccessToken(company.id!!, company.role)

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $companyAccessToken")
            .body("""{"likeStatus": "LIKE"}""")
            .`when`()
            .post("/api/links/${link.id}/like")
            .then()
            .statusCode(HttpStatus.OK.value())

        assertEquals(1, linkRepository.findById(link.id!!).orElseThrow().likeCount)
        assertEquals(1, linkDailyStatsRepository.findAll().single().likeCount)

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .delete("/admin/companies/${company.id}")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        assertTrue(linkRepository.existsById(link.id!!))
        assertEquals(0, linkRepository.findById(link.id!!).orElseThrow().likeCount)
        assertEquals(0, linkDailyStatsRepository.findAll().single().likeCount)
        assertNull(linkLikeLogRepository.findByLinkIdAndUserId(link.id!!, company.id!!))
    }

    @Test
    @DisplayName("ADMIN 권한은 회사를 삭제할 때 남아 있는 게시물과 댓글의 좋아요 통계를 되돌린다")
    fun adminCanDeleteCompanyAndAdjustSurvivingPostAndCommentLikeStats() {
        val company = saveCompanyUser("토스")
        val companyAccessToken = jwtTokenProvider.createAccessToken(company.id!!, company.role)
        val post =
            postRepository.saveAndFlush(
                Post(
                    title = "일반 사용자 게시물",
                    content = "게시물 본문",
                    author = normalUser,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        val comment =
            commentRepository.saveAndFlush(
                Comment(
                    content = "일반 사용자 댓글",
                    post = post,
                    author = normalUser,
                ),
            )

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $companyAccessToken")
            .body("""{"likeStatus": "LIKE"}""")
            .`when`()
            .post("/api/posts/${post.id}/like")
            .then()
            .statusCode(HttpStatus.OK.value())

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $companyAccessToken")
            .body("""{"likeStatus": "LIKE"}""")
            .`when`()
            .post("/api/comments/${comment.id}/like")
            .then()
            .statusCode(HttpStatus.OK.value())

        assertEquals(1, postRepository.findById(post.id!!).orElseThrow().likeCount)
        assertEquals(1, postDailyStatsRepository.findAll().single().likeCount)
        assertEquals(1, commentRepository.findById(comment.id!!).orElseThrow().likeCount)

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .delete("/admin/companies/${company.id}")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        assertTrue(postRepository.existsById(post.id!!))
        assertTrue(commentRepository.existsById(comment.id!!))
        assertEquals(0, postRepository.findById(post.id!!).orElseThrow().likeCount)
        assertEquals(0, postDailyStatsRepository.findAll().single().likeCount)
        assertEquals(0, commentRepository.findById(comment.id!!).orElseThrow().likeCount)
        assertNull(postLikeLogRepository.findByPostIdAndUserId(post.id!!, company.id!!))
        assertNull(commentLikeLogRepository.findByCommentIdAndUserId(comment.id!!, company.id!!))
    }

    @Test
    @DisplayName("ADMIN 권한은 회사를 삭제할 때 남의 게시물에 작성한 댓글 스레드를 보존한다")
    fun adminCanDeleteCompanyAndPreserveSurvivingCommentThreads() {
        val company = saveCompanyUser("토스")
        val post =
            postRepository.saveAndFlush(
                Post(
                    title = "일반 사용자 게시물",
                    content = "게시물 본문",
                    author = normalUser,
                    status = PostStatusEnum.PUBLISHED,
                    commentCount = 2,
                ),
            )
        val companyComment =
            commentRepository.saveAndFlush(
                Comment(
                    content = "회사 댓글",
                    post = post,
                    author = company,
                    replyCount = 1,
                ),
            )
        val normalReply =
            commentRepository.saveAndFlush(
                Comment(
                    content = "일반 사용자 답글",
                    post = post,
                    author = normalUser,
                    parent = companyComment,
                    depth = 1,
                ),
            )
        val statDate = DateUtils.toUtcDate(companyComment.createdAt)
        postDailyStatsRepository.saveAndFlush(
            PostDailyStats(
                post = post,
                statDate = statDate,
                commentCount = 2,
            ),
        )

        given()
            .header("Authorization", "Bearer $adminAccessToken")
            .`when`()
            .delete("/admin/companies/${company.id}")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        val deletedCompanyComment = commentRepository.findById(companyComment.id!!).orElseThrow()
        val survivingReply = commentRepository.findById(normalReply.id!!).orElseThrow()

        assertNotNull(deletedCompanyComment.deletedAt)
        assertNull(deletedCompanyComment.author)
        assertEquals(companyComment.id, survivingReply.parent?.id)
        assertEquals(1, postRepository.findById(post.id!!).orElseThrow().commentCount)
        assertEquals(1, postDailyStatsRepository.findAll().single().commentCount)
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

    @Test
    @DisplayName("ADMIN 권한은 회사 봇 영구 토큰을 발급하고 DB에 저장할 수 있다")
    fun adminCanCreateCompanyPermanentToken() {
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")

        val token =
            given()
                .contentType("application/json")
                .header("Authorization", "Bearer $adminAccessToken")
                .body(
                    """
                    {
                      "name": "토스 기술블로그 수집 봇"
                    }
                    """.trimIndent(),
                ).`when`()
                .post("/admin/companies/${companyUser.id}/tokens")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("data.id", notNullValue())
                .body("data.userId", equalTo(companyUser.id.toString()))
                .body("data.name", equalTo("토스 기술블로그 수집 봇"))
                .body("data.token", notNullValue())
                .body("data.permanent", equalTo(true))
                .body("data.expiredAt", nullValue())
                .extract()
                .path<String>("data.token")

        val savedToken = userTokenRepository.findAll().single()
        val payloadJson = decodeJwtPayload(token)

        assertEquals(companyUser.id, savedToken.user.id)
        assertEquals("토스 기술블로그 수집 봇", savedToken.name)
        assertEquals(jwtTokenProvider.hashToken(token), savedToken.tokenHash)
        assertTrue(payloadJson.contains("\"permanent\":true"))
        assertFalse(payloadJson.contains("\"exp\""))
    }

    @Test
    @DisplayName("회사 봇 영구 토큰을 재발급하면 기존 토큰은 삭제되고 새 토큰만 남는다")
    fun creatingCompanyPermanentTokenAgainReplacesExistingToken() {
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val firstToken = createCompanyToken(companyUser.id!!, "첫 번째 수집 봇")

        val secondToken = createCompanyToken(companyUser.id!!, "두 번째 수집 봇")

        val savedToken = userTokenRepository.findAll().single()
        assertEquals(companyUser.id, savedToken.user.id)
        assertEquals("두 번째 수집 봇", savedToken.name)
        assertEquals(jwtTokenProvider.hashToken(secondToken), savedToken.tokenHash)
        assertFalse(firstToken == secondToken)

        given()
            .header("Authorization", "Bearer $firstToken")
            .`when`()
            .get("/api/users/me")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())

        given()
            .header("Authorization", "Bearer $secondToken")
            .`when`()
            .get("/api/users/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(companyUser.id.toString()))
    }

    @Test
    @DisplayName("DB에 저장된 회사 봇 영구 토큰은 사용자 API 인증에 사용할 수 있다")
    fun registeredCompanyPermanentTokenCanAuthenticate() {
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val token = createCompanyToken(companyUser.id!!)

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/api/users/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(companyUser.id.toString()))
            .body("data.name", equalTo("토스"))
    }

    @Test
    @DisplayName("회사 역할이 변경되면 저장된 영구 토큰이 삭제되어 재승격 후에도 인증에 실패한다")
    fun companyPermanentTokenCannotAuthenticateAfterRoleChanged() {
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val token = createCompanyToken(companyUser.id!!)
        assertEquals(1, userTokenRepository.count())

        updateUserRole(companyUser.id!!, UserRole.USER)
        updateUserRole(companyUser.id!!, UserRole.COMPANY)

        assertEquals(0, userTokenRepository.count())
        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/api/users/me")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("DB에 저장되지 않은 회사 봇 영구 토큰은 인증에 실패한다")
    fun unregisteredCompanyPermanentTokenCannotAuthenticate() {
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val token = createCompanyToken(companyUser.id!!)
        userTokenRepository.deleteAllInBatch()

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/api/users/me")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
    }

    @Test
    @DisplayName("USER 권한은 회사 봇 영구 토큰을 발급할 수 없다")
    fun userCannotCreateCompanyPermanentToken() {
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")

        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $userAccessToken")
            .body(
                """
                {
                  "name": "토스 기술블로그 수집 봇"
                }
                """.trimIndent(),
            ).`when`()
            .post("/admin/companies/${companyUser.id}/tokens")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    private fun saveCompanyUser(
        name: String,
        identifier: String = "company-${UUID.randomUUID()}",
    ): User {
        return userRepository.save(
            User(
                name = name,
                email = "$identifier-${UUID.randomUUID()}@example.com",
                provider = OAuthProvider.SYSTEM,
                identifier = identifier,
                role = UserRole.COMPANY,
                profileImageUrl = "https://example.com/$identifier.png",
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
        userLinkRepository.save(UserLink(user = company, link = link, isSource = true))
        return link
    }

    private fun countLinkTags(linkId: UUID): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM link_tags WHERE link_id = ?",
            Int::class.java,
            linkId,
        ) ?: 0
    }

    private fun createCompanyToken(
        companyUserId: UUID,
        tokenName: String = "토스 기술블로그 수집 봇",
    ): String {
        return given()
            .contentType("application/json")
            .header("Authorization", "Bearer $adminAccessToken")
            .body(
                """
                {
                  "name": "$tokenName"
                }
                """.trimIndent(),
            ).`when`()
            .post("/admin/companies/$companyUserId/tokens")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .path("data.token")
    }

    private fun updateUserRole(
        userId: UUID,
        role: UserRole,
    ) {
        given()
            .contentType("application/json")
            .header("Authorization", "Bearer $adminAccessToken")
            .body(
                """
                {
                  "role": "${role.name}"
                }
                """.trimIndent(),
            ).`when`()
            .patch("/admin/users/$userId/role")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.role", equalTo(role.name))
    }

    private fun decodeJwtPayload(token: String): String {
        val payload = token.split(".")[1]
        return String(Base64.getUrlDecoder().decode(payload))
    }
}
