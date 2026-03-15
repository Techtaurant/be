package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.entity.UserFollow
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserFollowRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

@DisplayName("UserController follow 통합 테스트")
class UserControllerFollowIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userFollowRepository: UserFollowRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var firstTargetUser: User
    private lateinit var secondTargetUser: User
    private lateinit var followerUser: User
    private lateinit var accessToken: String

    @BeforeEach
    fun setUpTestData() {
        userFollowRepository.deleteAllInBatch()

        testUser = createUser(name = "테스트사용자", emailPrefix = "test")
        firstTargetUser = createUser(name = "첫번째대상", emailPrefix = "target1")
        secondTargetUser = createUser(name = "두번째대상", emailPrefix = "target2")
        followerUser = createUser(name = "팔로워사용자", emailPrefix = "follower")
        accessToken = jwtTokenProvider.createAccessToken(testUser.id!!, testUser.role)
    }

    @Test
    @DisplayName("사용자 팔로우가 성공한다")
    fun follow_success() {
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/users/${firstTargetUser.id}/follow")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.userId", equalTo(firstTargetUser.id.toString()))
            .body("data.name", equalTo(firstTargetUser.name))
    }

    @Test
    @DisplayName("팔로워 수와 팔로우 수 조회가 성공한다")
    fun getFollowCounts_success() {
        userFollowRepository.save(UserFollow(follower = testUser, following = firstTargetUser))
        userFollowRepository.save(UserFollow(follower = firstTargetUser, following = secondTargetUser))
        userFollowRepository.save(UserFollow(follower = followerUser, following = firstTargetUser))

        given()
            .`when`()
            .get("/open-api/users/${firstTargetUser.id}/follow-counts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.followerCount", equalTo(2))
            .body("data.followingCount", equalTo(1))
    }

    @Test
    @DisplayName("팔로잉 목록 조회가 성공한다")
    fun getFollowings_success() {
        userFollowRepository.save(UserFollow(follower = testUser, following = firstTargetUser))
        userFollowRepository.save(UserFollow(follower = testUser, following = secondTargetUser))

        given()
            .`when`()
            .get("/open-api/users/${testUser.id}/followings")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(2))
            .body("data.userId", hasItems(firstTargetUser.id.toString(), secondTargetUser.id.toString()))
    }

    @Test
    @DisplayName("팔로워 목록 조회가 성공한다")
    fun getFollowers_success() {
        userFollowRepository.save(UserFollow(follower = testUser, following = firstTargetUser))
        userFollowRepository.save(UserFollow(follower = followerUser, following = firstTargetUser))

        given()
            .`when`()
            .get("/open-api/users/${firstTargetUser.id}/followers")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(2))
            .body("data.userId", hasItems(testUser.id.toString(), followerUser.id.toString()))
    }

    @Test
    @DisplayName("팔로우 취소가 성공한다")
    fun unfollow_success() {
        userFollowRepository.save(UserFollow(follower = testUser, following = firstTargetUser))

        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/users/${firstTargetUser.id}/follow")
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value())

        given()
            .`when`()
            .get("/open-api/users/${testUser.id}/follow-counts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.followerCount", equalTo(0))
            .body("data.followingCount", equalTo(0))
    }

    private fun createUser(
        name: String,
        emailPrefix: String,
    ): User {
        return userRepository.save(
            User(
                name = name,
                email = "$emailPrefix-${UUID.randomUUID()}@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "$emailPrefix-id-${UUID.randomUUID()}",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/$emailPrefix.jpg",
            ),
        )
    }
}
