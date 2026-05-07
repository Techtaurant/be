package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.entity.PostLikeLog
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.entity.UserBan
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserBanRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
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
    private lateinit var postLikeLogRepository: PostLikeLogRepository

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    private lateinit var userBanRepository: UserBanRepository

    private lateinit var testUser: User
    private lateinit var otherUser: User
    private lateinit var accessToken: String

    @BeforeEach
    fun setUpTestData() {
        userBanRepository.deleteAllInBatch()
        postLikeLogRepository.deleteAllInBatch()
        postRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
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

    @Test
    @DisplayName("v2 게시물 목록은 인증 헤더가 있어도 공개 게시물만 반환하고 isRead를 포함하지 않는다")
    fun getPostsV2_withAuthentication_returnsOnlyPublicFields() {
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

        // when & then
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/open-api/v2/posts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.id", hasItem(myPublishedPost.id.toString()))
            .body("data.content.id", not(hasItem(myPrivatePost.id.toString())))
            .body("data.content[0].isRead", nullValue())
    }

    @Test
    @DisplayName("v2 게시물 상세는 likeStatus와 isRead를 포함하지 않는다")
    fun getPostDetailV2_returnsOnlyPublicFields() {
        // given
        val post =
            postRepository.save(
                Post(
                    title = "공개 게시물",
                    content = "공개 내용",
                    author = testUser,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        postLikeLogRepository.save(PostLikeLog(post = post, user = testUser, isLiked = true))

        // when & then
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/open-api/v2/posts/${post.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", org.hamcrest.Matchers.`is`(post.id.toString()))
            .body("data.likeStatus", nullValue())
            .body("data.isRead", nullValue())
    }

    @Test
    @DisplayName("로그인 사용자가 게시물 목록 조회 시 본인의 DRAFT 게시물은 포함되지 않는다")
    fun getPosts_loggedInUser_excludesOwnDraftPost() {
        // given
        val myDraftPost =
            postRepository.save(
                Post(
                    title = "내 임시 저장 게시물",
                    content = "임시 저장 내용",
                    author = testUser,
                    status = PostStatusEnum.DRAFT,
                ),
            )
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

        // when & then
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/open-api/posts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.id", not(hasItem(myDraftPost.id.toString())))
            .body("data.content.id", hasItem(myPrivatePost.id.toString()))
            .body("data.content.id", hasItem(myPublishedPost.id.toString()))
    }

    @Test
    @DisplayName("로그인 사용자가 차단한 작성자의 게시물은 목록에서 제외된다")
    fun getPosts_excludesBannedAuthorPosts() {
        // given
        val visiblePost =
            postRepository.save(
                Post(
                    title = "보이는 게시물",
                    content = "보이는 내용",
                    author = testUser,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        val bannedPost =
            postRepository.save(
                Post(
                    title = "숨겨질 게시물",
                    content = "숨겨질 내용",
                    author = otherUser,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        userBanRepository.save(UserBan(user = testUser, bannedUser = otherUser))

        // when & then
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/open-api/posts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.id", hasItem(visiblePost.id.toString()))
            .body("data.content.id", not(hasItem(bannedPost.id.toString())))
    }

    @Test
    @DisplayName("로그인 사용자가 차단한 작성자의 게시물 상세 조회 시 404를 반환한다")
    fun getPostDetail_bannedAuthor_returnsNotFound() {
        // given
        val bannedPost =
            postRepository.save(
                Post(
                    title = "차단된 사용자의 게시물",
                    content = "숨겨질 내용",
                    author = otherUser,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        userBanRepository.save(UserBan(user = testUser, bannedUser = otherUser))

        // when & then
        given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/open-api/posts/${bannedPost.id}")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }
}
