package com.techtaurant.mainserver.user.infrastructure.`in`

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.dto.ApiResponse
import com.techtaurant.mainserver.common.dto.CursorPageResponse
import com.techtaurant.mainserver.post.dto.PostListItemResponse
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.dto.UserResponse
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.restassured.RestAssured
import io.restassured.common.mapper.TypeRef
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("사용자 검색 API")
class UserOpenApiControllerTest : IntegrationTest() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setup() {
        postRepository.deleteAllInBatch()
        categoryRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    private fun createUser(name: String): User =
        userRepository.save(
            User(
                name = name,
                email = "${UUID.randomUUID()}@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "google-${UUID.randomUUID()}",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/profile.jpg",
            ),
        )

    private fun createCategory(
        user: User,
        name: String,
        path: String,
        depth: Int,
        parent: Category? = null,
    ): Category =
        categoryRepository.save(
            Category(
                user = user,
                name = name,
                path = path,
                depth = depth,
                parent = parent,
            ),
        )

    private fun createPost(
        author: User,
        title: String,
        category: Category? = null,
    ): Post =
        postRepository.save(
            Post(
                title = title,
                content = "게시물 내용",
                author = author,
                category = category,
                status = PostStatusEnum.PUBLISHED,
            ),
        )

    @Nested
    @DisplayName("입력값 검증")
    inner class ValidationTest {
        @Test
        @DisplayName("빈 문자열로 검색하면 400을 반환한다")
        fun whenNameIsEmpty_thenReturn400() {
            // given
            // 검색어 없이 빈 문자열 전달

            // when
            val statusCode =
                RestAssured
                    .given()
                    .queryParam("name", "")
                    .`when`()
                    .get("/open-api/users/search")
                    .then()
                    .extract()
                    .statusCode()

            // then
            assertEquals(400, statusCode, "빈 문자열 검색은 400을 반환해야 한다")
        }

        @Test
        @DisplayName("공백만 있는 문자열로 검색하면 400을 반환한다")
        fun whenNameIsBlank_thenReturn400() {
            // given
            // 공백 문자만으로 구성된 검색어 전달

            // when
            val statusCode =
                RestAssured
                    .given()
                    .queryParam("name", "   ")
                    .`when`()
                    .get("/open-api/users/search")
                    .then()
                    .extract()
                    .statusCode()

            // then
            assertEquals(400, statusCode, "공백만 있는 검색어는 400을 반환해야 한다")
        }
    }

    @Nested
    @DisplayName("정상 검색")
    inner class SearchTest {
        @Test
        @DisplayName("유효한 이름으로 검색하면 매칭되는 사용자 목록을 반환한다")
        fun whenNameIsValid_thenReturnMatchingUsers() {
            // given
            userRepository.save(
                User(
                    name = "김테크",
                    email = "kim@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "google-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )
            userRepository.save(
                User(
                    name = "이개발",
                    email = "lee@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "google-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )

            // when
            val response =
                RestAssured
                    .given()
                    .queryParam("name", "테크")
                    .`when`()
                    .get("/open-api/users/search")
                    .then()
                    .statusCode(200)
                    .extract()
                    .`as`(object : TypeRef<ApiResponse<List<UserResponse>>>() {})

            // then
            val users = response.data!!
            assertEquals(1, users.size, "검색어와 매칭되는 사용자 1명이 반환되어야 한다")
            assertEquals("김테크", users[0].name)
        }

        @Test
        @DisplayName("존재하지 않는 이름으로 검색하면 빈 목록을 반환한다")
        fun whenNameDoesNotMatch_thenReturnEmptyList() {
            // given
            userRepository.save(
                User(
                    name = "김테크",
                    email = "kim@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "google-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )

            // when
            val response =
                RestAssured
                    .given()
                    .queryParam("name", "존재하지않는이름xyz")
                    .`when`()
                    .get("/open-api/users/search")
                    .then()
                    .statusCode(200)
                    .extract()
                    .`as`(object : TypeRef<ApiResponse<List<UserResponse>>>() {})

            // then
            assertTrue(response.data!!.isEmpty(), "매칭되는 사용자가 없으면 빈 목록이 반환되어야 한다")
        }
    }

    @Nested
    @DisplayName("사용자 게시물 목록 조회")
    inner class UserPostsTest {
        @Test
        @DisplayName("카테고리가 있는 게시물 조회 시 category 정보가 함께 반환된다")
        fun getPostsByUserId_withCategory_returnsCategoryInResponse() {
            // given
            val author = createUser("김테크")
            val rootCategory = createCategory(author, name = "백엔드", path = "backend", depth = 1)
            val childCategory = createCategory(author, name = "Kotlin", path = "backend/kotlin", depth = 2, parent = rootCategory)
            val post = createPost(author = author, title = "카테고리 포함 게시물", category = childCategory)

            // when
            val response =
                RestAssured
                    .given()
                    .`when`()
                    .get("/open-api/users/${author.id}/posts")
                    .then()
                    .statusCode(200)
                    .extract()
                    .`as`(object : TypeRef<ApiResponse<CursorPageResponse<PostListItemResponse>>>() {})

            // then
            val returnedPost = response.data!!.content.single { it.id == post.id }
            assertEquals(childCategory.id, returnedPost.category?.id)
            assertEquals(childCategory.name, returnedPost.category?.name)
            assertEquals(childCategory.path, returnedPost.category?.path)
            assertEquals(childCategory.depth, returnedPost.category?.depth)
            assertEquals(rootCategory.id, returnedPost.category?.parentId)
        }
    }
}
