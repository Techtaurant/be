package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
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
import java.util.Base64
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("AdminCompanyController 통합 테스트")
class AdminCompanyControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userTokenRepository: UserTokenRepository

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
    @DisplayName("ADMIN 권한은 회사 봇 영구 토큰을 발급하고 DB에 저장할 수 있다")
    fun adminCanCreateCompanyPermanentToken() {
        // Given
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")

        // When
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

        // Then
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
        // Given
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val firstToken = createCompanyToken(companyUser.id!!, "첫 번째 수집 봇")

        // When
        val secondToken = createCompanyToken(companyUser.id!!, "두 번째 수집 봇")

        // Then
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
        // Given
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val token = createCompanyToken(companyUser.id!!)

        // When & Then
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
        // Given
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val token = createCompanyToken(companyUser.id!!)
        assertEquals(1, userTokenRepository.count())

        // When
        updateUserRole(companyUser.id!!, UserRole.USER)
        updateUserRole(companyUser.id!!, UserRole.COMPANY)

        // Then
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
        // Given
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")
        val token = createCompanyToken(companyUser.id!!)
        userTokenRepository.deleteAllInBatch()

        // When & Then
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
        // Given
        val companyUser = saveCompanyUser(name = "토스", identifier = "company-toss")

        // When & Then
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
        identifier: String,
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
