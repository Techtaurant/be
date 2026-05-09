package com.techtaurant.mainserver.post.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

@DisplayName("PostReadOpenApiV2Controller 통합 테스트")
class PostReadOpenApiV2ControllerIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    private lateinit var author: User

    @BeforeEach
    fun setUpTestData() {
        postRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()

        author =
            userRepository.save(
                User(
                    name = "작성자",
                    email = "writer-${UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "writer-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )
    }

    @Test
    @DisplayName("v2 게시물 목록은 정적 콘텐츠 필드만 반환한다")
    fun getPostContents_returnsStaticFieldsOnly() {
        // given
        val post =
            postRepository.save(
                Post(
                    title = "정적 콘텐츠 게시물",
                    content = "본문",
                    author = author,
                    viewCount = 10,
                    likeCount = 2,
                    commentCount = 1,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )

        // when & then
        given()
            .`when`()
            .get("/open-api/v2/posts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content[0].id", equalTo(post.id.toString()))
            .body("data.content[0].title", equalTo("정적 콘텐츠 게시물"))
            .body("data.content[0].viewCount", nullValue())
            .body("data.content[0].likeCount", nullValue())
            .body("data.content[0].commentCount", nullValue())
            .body("data.content[0].status", nullValue())
            .body("data.content[0].thumbnailUrl", nullValue())
            .body("data.content[0].authorProfileImageUrl", nullValue())
            .body("data.content[0].isRead", nullValue())
    }

    @Test
    @DisplayName("v2 게시물 상세 조회는 조회수를 증가시키지 않는다")
    fun getPostContentDetail_doesNotIncrementViewCount() {
        // given
        val post =
            postRepository.save(
                Post(
                    title = "상세 게시물",
                    content = "본문",
                    author = author,
                    viewCount = 0,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )

        // when & then
        given()
            .`when`()
            .get("/open-api/v2/posts/${post.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(post.id.toString()))
            .body("data.viewCount", nullValue())
            .body("data.attachmentPresignedUrls", nullValue())

        val updatedPost = postRepository.findById(post.id!!).orElseThrow()
        org.assertj.core.api.Assertions.assertThat(updatedPost.viewCount).isZero()
    }

    @Test
    @DisplayName("v2 공개 목록과 상세는 PUBLISHED 게시물만 반환한다")
    fun getPostContents_excludesNonPublishedPosts() {
        // given
        val publishedPost =
            postRepository.save(
                Post(
                    title = "공개 게시물",
                    content = "본문",
                    author = author,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )
        val privatePost =
            postRepository.save(
                Post(
                    title = "비공개 게시물",
                    content = "본문",
                    author = author,
                    status = PostStatusEnum.PRIVATE,
                ),
            )

        // when & then
        given()
            .`when`()
            .get("/open-api/v2/posts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content.id", hasItem(publishedPost.id.toString()))
            .body("data.content.id", not(hasItem(privatePost.id.toString())))

        given()
            .`when`()
            .get("/open-api/v2/posts/${privatePost.id}")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
    }
}
