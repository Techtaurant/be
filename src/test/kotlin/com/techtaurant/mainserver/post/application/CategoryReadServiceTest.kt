package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.infrastructure.out.CategoryPostCountProjection
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class CategoryReadServiceTest {
    private val categoryRepository: CategoryRepository = mockk()
    private val postRepository: PostRepository = mockk()

    private val categoryReadService =
        CategoryReadService(
            categoryRepository = categoryRepository,
            postRepository = postRepository,
        )

    private val user =
        User(
            name = "테스터",
            email = "tester@example.com",
            provider = OAuthProvider.GOOGLE,
            identifier = "tester-${UUID.randomUUID()}",
            role = UserRole.USER,
            profileImageUrl = "https://example.com/profile.png",
        ).apply {
            id = UUID.randomUUID()
        }

    private fun createCategory(
        name: String,
        path: String,
        depth: Int,
        parent: Category? = null,
    ): Category =
        Category(
            user = user,
            name = name,
            path = path,
            depth = depth,
            parent = parent,
        ).apply {
            id = UUID.randomUUID()
        }

    private fun createCategoryPostCountProjection(
        categoryId: UUID,
        postCount: Long,
    ): CategoryPostCountProjection =
        object : CategoryPostCountProjection {
            override fun getCategoryId(): UUID = categoryId

            override fun getPostCount(): Long = postCount
        }

    @Nested
    @DisplayName("searchByPath")
    inner class SearchByPath {
        @Test
        @DisplayName("카테고리별 게시물 개수를 함께 반환한다")
        fun searchByPath_returnsCategoriesWithPostCounts() {
            // given
            val rootCategory = createCategory(name = "java", path = "java", depth = 1)
            val childCategory = createCategory(name = "spring", path = "java/spring", depth = 2, parent = rootCategory)

            every { categoryRepository.findByUserIdAndPathPrefix(user.id!!, "java") } returns
                listOf(rootCategory, childCategory)
            every { postRepository.countByCategoryIds(listOf(rootCategory.id!!, childCategory.id!!)) } returns
                listOf(
                    createCategoryPostCountProjection(rootCategory.id!!, 2L),
                    createCategoryPostCountProjection(childCategory.id!!, 5L),
                )

            // when
            val result = categoryReadService.searchByPath(user.id!!, "java")

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0].id).isEqualTo(rootCategory.id)
            assertThat(result[0].postCount).isEqualTo(2L)
            assertThat(result[1].id).isEqualTo(childCategory.id)
            assertThat(result[1].parentId).isEqualTo(rootCategory.id)
            assertThat(result[1].postCount).isEqualTo(5L)
        }

        @Test
        @DisplayName("게시물이 없는 카테고리는 게시물 개수를 0으로 반환한다")
        fun searchByPath_returnsZeroWhenCategoryHasNoPosts() {
            // given
            val category = createCategory(name = "kotlin", path = "kotlin", depth = 1)

            every { categoryRepository.findByUserId(user.id!!) } returns listOf(category)
            every { postRepository.countByCategoryIds(listOf(category.id!!)) } returns emptyList()

            // when
            val result = categoryReadService.searchByPath(user.id!!, null)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].postCount).isZero()
        }

        @Test
        @DisplayName("카테고리가 없으면 게시물 집계 조회를 수행하지 않는다")
        fun searchByPath_skipsPostCountQueryWhenNoCategories() {
            // given
            every { categoryRepository.findByUserId(user.id!!) } returns emptyList()

            // when
            val result = categoryReadService.searchByPath(user.id!!, null)

            // then
            assertThat(result).isEmpty()
            verify(exactly = 0) { postRepository.countByCategoryIds(any()) }
        }
    }
}
