package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
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
import kotlin.test.assertEquals
import java.util.UUID

@DisplayName("AdminCompanyController 통합 테스트")
class AdminCompanyControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

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
}
