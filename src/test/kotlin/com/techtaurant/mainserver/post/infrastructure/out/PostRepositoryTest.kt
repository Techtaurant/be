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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@ActiveProfiles("test")
class PostRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: User
    private lateinit var testCategory: Category
    private lateinit var testPost: Post

    private fun createPost(
        title: String = "테스트 게시물",
        category: Category? = testCategory,
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
        testUser =
            userRepository.save(
                User(
                    name = "테스트 사용자",
                    email = "test@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "test-id-${java.util.UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )

        testCategory =
            categoryRepository.save(
                Category(
                    user = testUser,
                    name = "테스트 카테고리",
                    path = "테스트카테고리",
                    depth = 1,
                ),
            )

        testPost =
            createPost()
    }

    @Test
    @DisplayName("incrementCommentCount는 게시물의 commentCount를 1 증가시킨다")
    fun incrementCommentCount_increasesCountByOne() {
        // Given
        val initialCount = testPost.commentCount

        // When
        postRepository.incrementCommentCount(testPost.id!!)
        postRepository.flush()
        entityManager.clear()

        // Then
        val updatedPost = postRepository.findById(testPost.id!!).orElseThrow()
        assertThat(updatedPost.commentCount).isEqualTo(initialCount + 1)
    }

    @Test
    @DisplayName("incrementCommentCount는 여러 번 호출 시 누적하여 증가한다")
    fun incrementCommentCount_multipleIncrements_accumulatesCorrectly() {
        // Given
        val initialCount = testPost.commentCount
        val numberOfIncrements = 5

        // When
        repeat(numberOfIncrements) {
            postRepository.incrementCommentCount(testPost.id!!)
        }
        postRepository.flush()
        entityManager.clear()

        // Then
        val updatedPost = postRepository.findById(testPost.id!!).orElseThrow()
        assertThat(updatedPost.commentCount).isEqualTo(initialCount + numberOfIncrements)
    }

    @Test
    @DisplayName("incrementCommentCount는 초기값이 0이 아닌 게시물에도 정상 동작한다")
    fun incrementCommentCount_withNonZeroInitialCount_worksCorrectly() {
        // Given
        testPost =
            postRepository.save(
                Post(
                    title = "댓글이 있는 게시물",
                    content = "이미 댓글이 10개 있음",
                    author = testUser,
                    category = testCategory,
                    commentCount = 10,
                ),
            )
        val initialCount = testPost.commentCount

        // When
        postRepository.incrementCommentCount(testPost.id!!)
        postRepository.flush()
        entityManager.clear()

        // Then
        val updatedPost = postRepository.findById(testPost.id!!).orElseThrow()
        assertThat(updatedPost.commentCount).isEqualTo(initialCount + 1)
    }

    @Test
    @DisplayName("incrementCommentCount는 DB 레벨에서 원자적으로 처리되어 다른 필드에 영향을 주지 않는다")
    fun incrementCommentCount_atomicOperation_doesNotAffectOtherFields() {
        // Given
        val originalTitle = testPost.title
        val originalContent = testPost.content
        val originalViewCount = testPost.viewCount
        val originalLikeCount = testPost.likeCount

        // When
        postRepository.incrementCommentCount(testPost.id!!)
        postRepository.flush()
        entityManager.clear()

        // Then
        val updatedPost = postRepository.findById(testPost.id!!).orElseThrow()
        assertThat(updatedPost.title).isEqualTo(originalTitle)
        assertThat(updatedPost.content).isEqualTo(originalContent)
        assertThat(updatedPost.viewCount).isEqualTo(originalViewCount)
        assertThat(updatedPost.likeCount).isEqualTo(originalLikeCount)
        assertThat(updatedPost.commentCount).isEqualTo(1) // 0에서 1로 증가
    }

    @Test
    @DisplayName("존재하지 않는 게시물 ID로 incrementCommentCount 호출 시 아무 일도 일어나지 않는다")
    fun incrementCommentCount_withNonExistentPostId_doesNothing() {
        // Given
        val allPostsBefore = postRepository.findAll()
        val nonExistentPostId = UUID.randomUUID()

        // When
        postRepository.incrementCommentCount(nonExistentPostId)
        postRepository.flush()

        // Then
        val allPostsAfter = postRepository.findAll()
        assertThat(allPostsAfter).hasSize(allPostsBefore.size)

        // 기존 게시물의 commentCount는 변경되지 않음
        val unchangedPost = postRepository.findById(testPost.id!!).orElseThrow()
        assertThat(unchangedPost.commentCount).isEqualTo(0)
    }
}
