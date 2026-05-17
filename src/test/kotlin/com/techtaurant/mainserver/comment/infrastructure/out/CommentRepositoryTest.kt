package com.techtaurant.mainserver.comment.infrastructure.out

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
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

@Transactional
@ActiveProfiles("test")
class CommentRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: User
    private lateinit var testPost: Post

    @BeforeEach
    fun setUpTestData() {
        testUser =
            userRepository.save(
                User(
                    name = "테스트 사용자",
                    email = "comment-repository-${java.util.UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "comment-repository-${java.util.UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )

        val category =
            categoryRepository.save(
                Category(
                    user = testUser,
                    name = "댓글 테스트 카테고리",
                    path = "댓글-테스트-${java.util.UUID.randomUUID()}",
                    depth = 1,
                ),
            )

        testPost =
            postRepository.save(
                Post(
                    title = "댓글 테스트 게시물",
                    content = "댓글 테스트 내용",
                    author = testUser,
                    category = category,
                ),
            )
    }

    @Test
    @DisplayName("decrementReplyCount는 replyCount가 0이어도 음수로 만들지 않는다")
    fun decrementReplyCount_whenCountIsZero_shouldRemainZero() {
        // Given
        val parentComment = createComment(replyCount = 0)

        // When
        commentRepository.decrementReplyCount(parentComment.id!!)
        commentRepository.flush()
        entityManager.clear()

        // Then
        val updatedComment = commentRepository.findById(parentComment.id!!).orElseThrow()
        assertThat(updatedComment.replyCount).isZero()
    }

    @Test
    @DisplayName("decrementReplyCount는 replyCount를 1 감소시킨다")
    fun decrementReplyCount_whenCountIsPositive_shouldDecreaseByOne() {
        // Given
        val parentComment = createComment(replyCount = 2)

        // When
        commentRepository.decrementReplyCount(parentComment.id!!)
        commentRepository.flush()
        entityManager.clear()

        // Then
        val updatedComment = commentRepository.findById(parentComment.id!!).orElseThrow()
        assertThat(updatedComment.replyCount).isEqualTo(1)
    }

    private fun createComment(replyCount: Long): Comment =
        commentRepository.save(
            Comment(
                content = "부모 댓글",
                post = testPost,
                author = testUser,
                replyCount = replyCount,
            ),
        )
}
