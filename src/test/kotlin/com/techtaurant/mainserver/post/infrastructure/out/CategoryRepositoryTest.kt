package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@Transactional
@ActiveProfiles("test")
class CategoryRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: User

    private fun createUser(identifier: String): User =
        userRepository.save(
            User(
                name = "테스트 사용자",
                email = "$identifier@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = identifier,
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
        category: Category?,
        title: String = "테스트 게시물",
    ): Post =
        postRepository.save(
            Post(
                title = title,
                content = "테스트 내용",
                author = testUser,
                category = category,
                commentCount = 0,
            ),
        )

    @BeforeEach
    fun setUpTestData() {
        testUser = createUser("test-user-${java.util.UUID.randomUUID()}")
    }

    @Nested
    @DisplayName("findByUserIdWithPostCount")
    inner class FindByUserIdWithPostCount {
        @Test
        @DisplayName("게시물이 있는 카테고리와 없는 카테고리 모두 반환하고 하위 카테고리까지 포함해 게시물 수를 집계한다")
        fun findByUserIdWithPostCount_returnsAllCategoriesWithAccuratePostCounts() {
            // given
            val javaCategory = createCategory(testUser, "java", "java", 1)
            val springCategory = createCategory(testUser, "spring", "java/spring", 2, javaCategory)
            val kotlinCategory = createCategory(testUser, "kotlin", "kotlin", 1)
            createPost(springCategory, "spring 게시물 1")
            createPost(springCategory, "spring 게시물 2")
            postRepository.flush()
            entityManager.clear()

            // when
            val result =
                categoryRepository.findByUserIdWithPostCount(testUser.id!!)
                    .associateBy { it.getId() }

            // then
            assertThat(result).hasSize(3)
            assertThat(result[javaCategory.id!!]!!.getPostCount()).isEqualTo(2L)
            assertThat(result[kotlinCategory.id!!]!!.getPostCount()).isZero()
            assertThat(result[springCategory.id!!]!!.getPostCount()).isEqualTo(2L)
        }

        @Test
        @DisplayName("다른 유저의 카테고리는 결과에 포함하지 않는다")
        fun findByUserIdWithPostCount_excludesOtherUsersCategories() {
            // given
            val otherUser = createUser("other-user-${java.util.UUID.randomUUID()}")
            createCategory(testUser, "java", "java", 1)
            createCategory(otherUser, "kotlin", "kotlin", 1)
            postRepository.flush()
            entityManager.clear()

            // when
            val result = categoryRepository.findByUserIdWithPostCount(testUser.id!!)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].getName()).isEqualTo("java")
        }

        @Test
        @DisplayName("카테고리가 없으면 빈 목록을 반환한다")
        fun findByUserIdWithPostCount_returnsEmptyWhenNoCategories() {
            // given — testUser에 카테고리 없음

            // when
            val result = categoryRepository.findByUserIdWithPostCount(testUser.id!!)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("미분류 게시물(category=null)은 집계에 포함하지 않는다")
        fun findByUserIdWithPostCount_excludesUncategorizedPosts() {
            // given
            val javaCategory = createCategory(testUser, "java", "java", 1)
            createPost(javaCategory, "분류된 게시물")
            createPost(null, "미분류 게시물")
            postRepository.flush()
            entityManager.clear()

            // when
            val result = categoryRepository.findByUserIdWithPostCount(testUser.id!!)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].getPostCount()).isEqualTo(1L)
        }

        @Test
        @DisplayName("부모 카테고리 ID를 정확하게 반환한다")
        fun findByUserIdWithPostCount_returnsCorrectParentId() {
            // given
            val parentCategory = createCategory(testUser, "java", "java", 1)
            val childCategory = createCategory(testUser, "spring", "java/spring", 2, parentCategory)
            postRepository.flush()
            entityManager.clear()

            // when
            val result =
                categoryRepository.findByUserIdWithPostCount(testUser.id!!)
                    .associateBy { it.getId() }

            // then
            assertThat(result[parentCategory.id!!]!!.getParentId()).isNull()
            assertThat(result[childCategory.id!!]!!.getParentId()).isEqualTo(parentCategory.id)
        }

        @Test
        @DisplayName("depth 오름차순, 이름 오름차순으로 정렬하여 반환한다")
        fun findByUserIdWithPostCount_returnsSortedByDepthThenName() {
            // given
            val parentCategory = createCategory(testUser, "java", "java", 1)
            createCategory(testUser, "spring", "java/spring", 2, parentCategory)
            createCategory(testUser, "kotlin", "kotlin", 1)
            postRepository.flush()
            entityManager.clear()

            // when
            val result = categoryRepository.findByUserIdWithPostCount(testUser.id!!)

            // then — depth 1: java, kotlin / depth 2: spring
            assertThat(result).hasSize(3)
            assertThat(result[0].getName()).isEqualTo("java")
            assertThat(result[1].getName()).isEqualTo("kotlin")
            assertThat(result[2].getName()).isEqualTo("spring")
        }
    }

    @Nested
    @DisplayName("findByUserIdAndPathPrefixWithPostCount")
    inner class FindByUserIdAndPathPrefixWithPostCount {
        @Test
        @DisplayName("prefix에 해당하는 카테고리만 게시물 수와 함께 반환한다")
        fun findByUserIdAndPathPrefixWithPostCount_returnsOnlyMatchingCategoriesWithPostCounts() {
            // given
            val javaCategory = createCategory(testUser, "java", "java", 1)
            val springCategory = createCategory(testUser, "spring", "java/spring", 2, javaCategory)
            createCategory(testUser, "kotlin", "kotlin", 1)
            createPost(javaCategory, "java 게시물")
            createPost(springCategory, "spring 게시물 1")
            createPost(springCategory, "spring 게시물 2")
            postRepository.flush()
            entityManager.clear()

            // when
            val result =
                categoryRepository.findByUserIdAndPathPrefixWithPostCount(testUser.id!!, "java")
                    .associateBy { it.getId() }

            // then
            assertThat(result).hasSize(2)
            assertThat(result).containsOnlyKeys(javaCategory.id!!, springCategory.id!!)
            assertThat(result[javaCategory.id!!]!!.getPostCount()).isEqualTo(3L)
            assertThat(result[springCategory.id!!]!!.getPostCount()).isEqualTo(2L)
        }

        @Test
        @DisplayName("일치하는 카테고리가 없으면 빈 목록을 반환한다")
        fun findByUserIdAndPathPrefixWithPostCount_returnsEmptyWhenNoCategoriesMatch() {
            // given
            createCategory(testUser, "kotlin", "kotlin", 1)
            postRepository.flush()
            entityManager.clear()

            // when
            val result = categoryRepository.findByUserIdAndPathPrefixWithPostCount(testUser.id!!, "java")

            // then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("게시물이 없는 카테고리는 게시물 수를 0으로 반환한다")
        fun findByUserIdAndPathPrefixWithPostCount_returnsZeroForCategoryWithNoPosts() {
            // given
            createCategory(testUser, "java", "java", 1)
            postRepository.flush()
            entityManager.clear()

            // when
            val result = categoryRepository.findByUserIdAndPathPrefixWithPostCount(testUser.id!!, "java")

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].getPostCount()).isZero()
        }

        @Test
        @DisplayName("상위 카테고리의 게시물 수는 하위 카테고리 게시물까지 포함한다")
        fun findByUserIdAndPathPrefixWithPostCount_includesDescendantPostsInParentCount() {
            // given
            val backendCategory = createCategory(testUser, "backend", "backend", 1)
            val javaCategory = createCategory(testUser, "java", "backend/java", 2, backendCategory)
            val springCategory = createCategory(testUser, "spring", "backend/java/spring", 3, javaCategory)
            createPost(springCategory, "spring 게시물 1")
            createPost(springCategory, "spring 게시물 2")
            createPost(javaCategory, "java 게시물 1")
            postRepository.flush()
            entityManager.clear()

            // when
            val result =
                categoryRepository.findByUserIdAndPathPrefixWithPostCount(testUser.id!!, "backend")
                    .associateBy { it.getId() }

            // then
            assertThat(result[backendCategory.id!!]!!.getPostCount()).isEqualTo(3L)
            assertThat(result[javaCategory.id!!]!!.getPostCount()).isEqualTo(3L)
            assertThat(result[springCategory.id!!]!!.getPostCount()).isEqualTo(2L)
        }
    }
}
