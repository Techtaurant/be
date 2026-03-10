package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserBanRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

@DisplayName("UserController ban 통합 테스트")
class UserControllerBanIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userBanRepository: UserBanRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var targetUser: User
    private lateinit var accessToken: String

    @BeforeEach
    fun setUpTestData() {
        userBanRepository.deleteAllInBatch()

        testUser =
            userRepository.save(
                User(
                    name = "테스트사용자",
                    email = "test@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "test-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )
        targetUser =
            userRepository.save(
                User(
                    name = "차단대상",
                    email = "target@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "target-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/target.jpg",
                ),
            )
        accessToken = jwtTokenProvider.createAccessToken(testUser.id!!, testUser.role)
    }

    @Test
    @DisplayName("사용자 차단 후 목록 조회와 차단 해제가 정상 동작한다")
    fun banListAndUnban_success() {
        // When - 차단 요청
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/users/${targetUser.id}/ban")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.userId", equalTo(targetUser.id.toString()))
            .body("data.name", equalTo(targetUser.name))

        // Then - 차단 목록 조회
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/users/me/bans")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(1))
            .body("data[0].userId", equalTo(targetUser.id.toString()))

        // When - 차단 해제
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/users/${targetUser.id}/ban")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        // Then - 차단 목록 비어 있음
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/users/me/bans")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(0))
    }
}
