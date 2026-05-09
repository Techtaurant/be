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
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

@DisplayName("PostMetadataOpenApiController 통합 테스트")
class PostMetadataOpenApiControllerIntegrationTest : IntegrationTest() {
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
    @DisplayName("metadata는 공개 게시물의 카운트와 URL 메타데이터를 반환한다")
    fun getPostMetadata_returnsPublishedPostMetadata() {
        // given
        val publishedPost =
            postRepository.save(
                Post(
                    title = "공개 게시물",
                    content = "본문",
                    author = author,
                    viewCount = 10,
                    likeCount = 2,
                    commentCount = 1,
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
            .queryParam("postIds", publishedPost.id, privatePost.id)
            .`when`()
            .get("/open-api/posts/metadata")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.postId", hasItem(publishedPost.id.toString()))
            .body("data.postId", not(hasItem(privatePost.id.toString())))
            .body("data.find { it.postId == '${publishedPost.id}' }.viewCount", equalTo(10))
            .body("data.find { it.postId == '${publishedPost.id}' }.likeCount", equalTo(2))
            .body("data.find { it.postId == '${publishedPost.id}' }.commentCount", equalTo(1))
            .body("data.find { it.postId == '${publishedPost.id}' }.status", equalTo("PUBLISHED"))
            .body("data.find { it.postId == '${publishedPost.id}' }.thumbnailUrl", notNullValue())
            .body("data.find { it.postId == '${publishedPost.id}' }.authorProfileImageUrl", equalTo(author.profileImageUrl))
    }

    @Test
    @DisplayName("metadata 작성자 프로필 이미지가 비어 있으면 기본 사용자 썸네일 URL을 반환한다")
    fun getPostMetadata_blankAuthorProfileImageUrl_returnsDefaultUserThumbnailUrl() {
        // given
        author.profileImageUrl = ""
        userRepository.save(author)
        val publishedPost =
            postRepository.save(
                Post(
                    title = "공개 게시물",
                    content = "본문",
                    author = author,
                    status = PostStatusEnum.PUBLISHED,
                ),
            )

        // when & then
        given()
            .queryParam("postIds", publishedPost.id)
            .`when`()
            .get("/open-api/posts/metadata")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body(
                "data.find { it.postId == '${publishedPost.id}' }.authorProfileImageUrl",
                equalTo("http://localhost:8080/static/images/user-thumbnail.png"),
            )
    }
}
