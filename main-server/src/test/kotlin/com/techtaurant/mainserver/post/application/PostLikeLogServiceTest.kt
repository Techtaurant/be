package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@DisplayName("PostLikeLogService 통합 테스트")
@Transactional
class PostLikeLogServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var postLikeLogService: PostLikeLogService

    @Autowired
    private lateinit var postLikeLogRepository: PostLikeLogRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postDailyStatsRepository: PostDailyStatsRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @DisplayName("새로운 좋아요를 생성하면 로그가 저장되고 카운트가 증가한다")
    fun recordLike_whenNewLike_shouldCreateLogAndIncrementCount() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val initialLikeCount = post.likeCount

        // when
        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = true)
        entityManager.flush()
        entityManager.clear()

        // then
        val savedLog = postLikeLogRepository.findByPostIdAndUserId(post.id!!, user.id!!)
        assertThat(savedLog).isNotNull
        assertThat(savedLog?.isLiked).isTrue

        val updatedPost = postRepository.findById(post.id!!).orElseThrow()
        assertThat(updatedPost.likeCount).isEqualTo(initialLikeCount + 1)
    }

    @Test
    @DisplayName("새로운 싫어요를 생성하면 로그가 저장되지만 카운트는 증가하지 않는다")
    fun recordLike_whenNewDislike_shouldCreateLogWithoutIncrementingCount() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val initialLikeCount = post.likeCount

        // when
        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = false)
        entityManager.flush()
        entityManager.clear()

        // then
        val savedLog = postLikeLogRepository.findByPostIdAndUserId(post.id!!, user.id!!)
        assertThat(savedLog).isNotNull
        assertThat(savedLog?.isLiked).isFalse

        val updatedPost = postRepository.findById(post.id!!).orElseThrow()
        assertThat(updatedPost.likeCount).isEqualTo(initialLikeCount)
    }

    @Test
    @DisplayName("좋아요를 싫어요로 변경하면 카운트가 감소한다")
    fun recordLike_whenChangeLikeToDislike_shouldDecrementCount() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)

        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = true)
        entityManager.flush()
        entityManager.clear()
        val likeCountAfterLike = postRepository.findById(post.id!!).orElseThrow().likeCount

        // when
        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = false)
        entityManager.flush()
        entityManager.clear()

        // then
        val updatedLog = postLikeLogRepository.findByPostIdAndUserId(post.id!!, user.id!!)
        assertThat(updatedLog?.isLiked).isFalse

        val updatedPost = postRepository.findById(post.id!!).orElseThrow()
        assertThat(updatedPost.likeCount).isEqualTo(likeCountAfterLike - 1)
    }

    @Test
    @DisplayName("싫어요를 좋아요로 변경하면 카운트가 증가한다")
    fun recordLike_whenChangeDislikeToLike_shouldIncrementCount() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)

        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = false)
        entityManager.flush()
        entityManager.clear()
        val likeCountAfterDislike = postRepository.findById(post.id!!).orElseThrow().likeCount

        // when
        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = true)
        entityManager.flush()
        entityManager.clear()

        // then
        val updatedLog = postLikeLogRepository.findByPostIdAndUserId(post.id!!, user.id!!)
        assertThat(updatedLog?.isLiked).isTrue

        val updatedPost = postRepository.findById(post.id!!).orElseThrow()
        assertThat(updatedPost.likeCount).isEqualTo(likeCountAfterDislike + 1)
    }

    @Test
    @DisplayName("동일한 좋아요 상태로 재요청하면 카운트가 변경되지 않는다")
    fun recordLike_whenSameLikeStatus_shouldNotChangeCount() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)

        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = true)
        entityManager.flush()
        entityManager.clear()
        val likeCountAfterFirstLike = postRepository.findById(post.id!!).orElseThrow().likeCount

        // when
        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = true)
        entityManager.flush()
        entityManager.clear()

        // then
        val updatedPost = postRepository.findById(post.id!!).orElseThrow()
        assertThat(updatedPost.likeCount).isEqualTo(likeCountAfterFirstLike)
    }

    @Test
    @DisplayName("동일한 싫어요 상태로 재요청하면 카운트가 변경되지 않는다")
    fun recordLike_whenSameDislikeStatus_shouldNotChangeCount() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)

        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = false)
        entityManager.flush()
        entityManager.clear()
        val likeCountAfterFirstDislike = postRepository.findById(post.id!!).orElseThrow().likeCount

        // when
        postLikeLogService.recordLike(post.id!!, user.id!!, isLiked = false)
        entityManager.flush()
        entityManager.clear()

        // then
        val updatedPost = postRepository.findById(post.id!!).orElseThrow()
        assertThat(updatedPost.likeCount).isEqualTo(likeCountAfterFirstDislike)
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 요청 시 예외가 발생한다")
    fun recordLike_whenPostNotFound_shouldThrowException() {
        // given
        val user = createAndSaveUser("test@example.com")
        val nonExistentPostId = UUID.randomUUID()

        // when & then
        assertThatThrownBy {
            postLikeLogService.recordLike(nonExistentPostId, user.id!!, isLiked = true)
        }.isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 좋아요 요청 시 예외가 발생한다")
    fun recordLike_whenUserNotFound_shouldThrowException() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val nonExistentUserId = UUID.randomUUID()

        // when & then
        assertThatThrownBy {
            postLikeLogService.recordLike(post.id!!, nonExistentUserId, isLiked = true)
        }.isInstanceOf(ApiException::class.java)
    }

    private fun createAndSaveUser(email: String): User {
        val user =
            User(
                name = "테스트 사용자",
                email = email,
                provider = OAuthProvider.GOOGLE,
                identifier = "google_$email",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/profile.jpg",
            )
        return userRepository.save(user)
    }

    private fun createAndSavePost(
        title: String,
        author: User,
    ): Post {
        val post =
            Post(
                title = title,
                content = "테스트 게시글 내용입니다.",
                author = author,
                status = PostStatusEnum.PUBLISHED,
            )
        return postRepository.save(post)
    }
}
