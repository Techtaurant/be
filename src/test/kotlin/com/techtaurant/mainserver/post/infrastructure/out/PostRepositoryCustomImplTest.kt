package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@ActiveProfiles("test")
class PostRepositoryCustomImplTest : IntegrationTest() {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var userA: User
    private lateinit var userB: User
    private lateinit var category: Category

    @BeforeEach
    fun setUpTestData() {
        postRepository.deleteAll()

        userA =
            userRepository.save(
                User(
                    name = "사용자A",
                    email = "a@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "user-a-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/a.jpg",
                ),
            )
        userB =
            userRepository.save(
                User(
                    name = "사용자B",
                    email = "b@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "user-b-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/b.jpg",
                ),
            )
        category =
            categoryRepository.save(
                Category(
                    user = userA,
                    name = "테스트 카테고리",
                    path = "테스트카테고리",
                    depth = 1,
                ),
            )
    }

    private fun createPost(
        author: User,
        status: PostStatusEnum = PostStatusEnum.PUBLISHED,
        postCategory: Category? = null,
    ): Post =
        postRepository.save(
            Post(
                title = "게시물",
                content = "내용",
                author = author,
                status = status,
                category = postCategory,
            ),
        )

    @Nested
    @DisplayName("authorId 필터링")
    inner class AuthorIdFilter {
        @Test
        @DisplayName("authorId 지정 시 해당 작성자의 게시물만 반환한다")
        fun findPostsWithConditions_withAuthorId_returnsOnlyAuthorPosts() {
            // given
            val postByA = createPost(userA)
            createPost(userB)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = userA.id!!,
                )

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(postByA.id)
        }

        @Test
        @DisplayName("authorId가 null이면 모든 작성자의 게시물을 반환한다")
        fun findPostsWithConditions_withoutAuthorId_returnsAllPosts() {
            // given
            createPost(userA)
            createPost(userB)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = null,
                )

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("statuses 필터링")
    inner class StatusesFilter {
        @Test
        @DisplayName("statuses가 null이면 PUBLISHED만 반환한다")
        fun findPostsWithConditions_withoutStatuses_returnsPublishedOnly() {
            // given
            val published = createPost(userA, PostStatusEnum.PUBLISHED)
            createPost(userA, PostStatusEnum.DRAFT)
            createPost(userA, PostStatusEnum.PRIVATE)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    statuses = null,
                )

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(published.id)
        }

        @Test
        @DisplayName("모든 상태를 지정하면 전체 게시물을 반환한다")
        fun findPostsWithConditions_withAllStatuses_returnsAllPosts() {
            // given
            createPost(userA, PostStatusEnum.PUBLISHED)
            createPost(userA, PostStatusEnum.DRAFT)
            createPost(userA, PostStatusEnum.PRIVATE)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    statuses = PostStatusEnum.entries,
                )

            // then
            assertThat(result).hasSize(3)
        }

        @Test
        @DisplayName("PUBLISHED만 지정하면 PUBLISHED 게시물만 반환한다")
        fun findPostsWithConditions_withPublishedOnly_returnsPublishedPosts() {
            // given
            val published = createPost(userA, PostStatusEnum.PUBLISHED)
            createPost(userA, PostStatusEnum.DRAFT)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                )

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(published.id)
        }
    }

    @Nested
    @DisplayName("categoryId 필터링")
    inner class CategoryIdFilter {
        @Test
        @DisplayName("categoryId 지정 시 해당 카테고리의 게시물만 반환한다")
        fun findPostsWithConditions_withCategoryId_returnsOnlyCategoryPosts() {
            // given
            val categorizedPost = createPost(userA, postCategory = category)
            createPost(userA)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    categoryId = category.id!!,
                )

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(categorizedPost.id)
        }

        @Test
        @DisplayName("categoryId가 null이면 모든 카테고리의 게시물을 반환한다")
        fun findPostsWithConditions_withoutCategoryId_returnsAllPosts() {
            // given
            createPost(userA, postCategory = category)
            createPost(userA)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    categoryId = null,
                )

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("복합 필터링")
    inner class CombinedFilter {
        @Test
        @DisplayName("authorId + statuses + categoryId를 함께 적용하면 교집합 결과를 반환한다")
        fun findPostsWithConditions_combinedFilters_returnsIntersection() {
            // given
            val target = createPost(userA, PostStatusEnum.PUBLISHED, category)
            createPost(userA, PostStatusEnum.DRAFT, category)
            createPost(userB, PostStatusEnum.PUBLISHED, category)
            createPost(userA, PostStatusEnum.PUBLISHED)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = userA.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = category.id!!,
                )

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(target.id)
        }
    }

    @Nested
    @DisplayName("visibleToUserId 필터링")
    inner class VisibleToUserIdFilter {
        @Test
        @DisplayName("visibleToUserId 지정 시 PUBLISHED + 해당 사용자의 모든 상태 게시물을 반환한다")
        fun findPostsWithConditions_withVisibleToUserId_returnsPublishedAndOwnPosts() {
            // given
            val publishedByA = createPost(userA, PostStatusEnum.PUBLISHED)
            val draftByA = createPost(userA, PostStatusEnum.DRAFT)
            val privateByA = createPost(userA, PostStatusEnum.PRIVATE)
            val publishedByB = createPost(userB, PostStatusEnum.PUBLISHED)
            createPost(userB, PostStatusEnum.DRAFT)
            createPost(userB, PostStatusEnum.PRIVATE)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = userA.id!!,
                )

            // then
            val resultIds = result.map { it.id }.toSet()
            assertThat(resultIds).containsExactlyInAnyOrder(
                publishedByA.id,
                draftByA.id,
                privateByA.id,
                publishedByB.id,
            )
        }

        @Test
        @DisplayName("visibleToUserId가 null이면 PUBLISHED만 반환한다")
        fun findPostsWithConditions_withoutVisibleToUserId_returnsPublishedOnly() {
            // given
            val publishedByA = createPost(userA, PostStatusEnum.PUBLISHED)
            createPost(userA, PostStatusEnum.DRAFT)
            createPost(userA, PostStatusEnum.PRIVATE)
            val publishedByB = createPost(userB, PostStatusEnum.PUBLISHED)
            createPost(userB, PostStatusEnum.DRAFT)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                )

            // then
            val resultIds = result.map { it.id }.toSet()
            assertThat(resultIds).containsExactlyInAnyOrder(
                publishedByA.id,
                publishedByB.id,
            )
        }

        @Test
        @DisplayName("visibleToUserId는 statuses보다 우선 적용된다")
        fun findPostsWithConditions_visibleToUserIdOverridesStatuses() {
            // given
            val publishedByA = createPost(userA, PostStatusEnum.PUBLISHED)
            val draftByA = createPost(userA, PostStatusEnum.DRAFT)
            val publishedByB = createPost(userB, PostStatusEnum.PUBLISHED)
            createPost(userB, PostStatusEnum.DRAFT)

            // when
            val result =
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 10,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    visibleToUserId = userA.id!!,
                )

            // then
            val resultIds = result.map { it.id }.toSet()
            assertThat(resultIds).containsExactlyInAnyOrder(
                publishedByA.id,
                draftByA.id,
                publishedByB.id,
            )
        }
    }
}
