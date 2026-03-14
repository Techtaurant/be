package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.CategoryWithPostCountProjection
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class CategoryReadServiceTest {
    private val categoryRepository: CategoryRepository = mockk()

    private val categoryReadService =
        CategoryReadService(
            categoryRepository = categoryRepository,
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

    private fun createProjection(
        id: UUID,
        name: String,
        path: String,
        depth: Int,
        parentId: UUID?,
        postCount: Long,
    ): CategoryWithPostCountProjection =
        object : CategoryWithPostCountProjection {
            override fun getId(): UUID = id

            override fun getName(): String = name

            override fun getPath(): String = path

            override fun getDepth(): Int = depth

            override fun getParentId(): UUID? = parentId

            override fun getPostCount(): Long = postCount
        }

    @Nested
    @DisplayName("searchByPath")
    inner class SearchByPath {
        @Test
        @DisplayName("카테고리별 게시물 개수를 함께 반환한다")
        fun searchByPath_returnsCategoriesWithPostCounts() {
            // given
            val rootId = UUID.randomUUID()
            val childId = UUID.randomUUID()
            every { categoryRepository.findByUserIdAndPathPrefixWithPostCount(user.id!!, "java") } returns
                listOf(
                    createProjection(rootId, "java", "java", 1, null, 2L),
                    createProjection(childId, "spring", "java/spring", 2, rootId, 5L),
                )

            // when
            val result = categoryReadService.searchByPath(user.id!!, "java")

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0].id).isEqualTo(rootId)
            assertThat(result[0].postCount).isEqualTo(2L)
            assertThat(result[1].id).isEqualTo(childId)
            assertThat(result[1].parentId).isEqualTo(rootId)
            assertThat(result[1].postCount).isEqualTo(5L)
        }

        @Test
        @DisplayName("게시물이 없는 카테고리는 게시물 개수를 0으로 반환한다")
        fun searchByPath_returnsZeroWhenCategoryHasNoPosts() {
            // given
            val categoryId = UUID.randomUUID()
            every { categoryRepository.findByUserIdWithPostCount(user.id!!) } returns
                listOf(createProjection(categoryId, "kotlin", "kotlin", 1, null, 0L))

            // when
            val result = categoryReadService.searchByPath(user.id!!, null)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].postCount).isZero()
        }

        @Test
        @DisplayName("카테고리가 없으면 빈 목록을 반환한다")
        fun searchByPath_returnsEmptyWhenNoCategories() {
            // given
            every { categoryRepository.findByUserIdWithPostCount(user.id!!) } returns emptyList()

            // when
            val result = categoryReadService.searchByPath(user.id!!, null)

            // then
            assertThat(result).isEmpty()
        }
    }
}
