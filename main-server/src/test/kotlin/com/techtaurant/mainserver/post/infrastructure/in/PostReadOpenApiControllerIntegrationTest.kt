package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

@DisplayName("PostReadOpenApiController 통합 테스트")
class PostReadOpenApiControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var testUser: User
    private lateinit var otherUser: User
    private lateinit var accessToken: String

    @BeforeEach
    fun setUpTestData() {
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

        otherUser =
            userRepository.save(
                User(
                    name = "다른사용자",
                    email = "other@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "other-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/other-profile.jpg",
                ),
            )

        accessToken = jwtTokenProvider.createAccessToken(testUser.id!!, testUser.role)
    }

    @Test
    @DisplayName("로그인 사용자가 게시물 목록 조회 시 본인의 PRIVATE 게시물이 포함된다")
    fun getPosts_loggedInUser_includesOwnPrivatePost() {
        // given
        val myPrivatePost =
            postRepository.save(
                Post(
                    title = "내 비공개 게시물",
                    content = "비공개 내용",
                    author = testUser,
                    status = PostStatusEnum.PRIVATE,
                ),
            )
        val myPublishedPost =
            postRepository.save(
                Post(
                    title = "내 공개 게시물",
                    content = "공개 내용",
                    author = testUser,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        val otherPrivatePost =
            postRepository.save(
                Post(
                    title = "타인 비공개 게시물",
                    content = "타인 비공개 내용",
                    author = otherUser,
                    status = PostStatusEnum.PRIVATE,
                ),
            )

        // when & then
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/open-api/posts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.id", hasItem(myPrivatePost.id.toString()))
            .body("data.content.id", hasItem(myPublishedPost.id.toString()))
            .body("data.content.id", not(hasItem(otherPrivatePost.id.toString())))
    }
}
