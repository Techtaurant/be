package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostDailyStats
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.sql.Date

@DisplayName("PostDailyStatsService 통합 테스트")
@Transactional
class PostDailyStatsServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var postDailyStatsService: PostDailyStatsService

    @Autowired
    private lateinit var postDailyStatsRepository: PostDailyStatsRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @DisplayName("레코드가 없을 때 조회수 증가하면 레코드가 생성되고 1이 된다")
    fun incrementViewCount_whenNoRecord_shouldCreateRecordWithCount1() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()

        // when
        postDailyStatsService.incrementViewCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats).isNotNull
        assertThat(dailyStats?.viewCount).isEqualTo(1)
    }

    @Test
    @DisplayName("레코드가 없을 때 좋아요수 증가하면 레코드가 생성되고 1이 된다")
    fun incrementLikeCount_whenNoRecord_shouldCreateRecordWithCount1() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()

        // when
        postDailyStatsService.incrementLikeCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats).isNotNull
        assertThat(dailyStats?.likeCount).isEqualTo(1)
    }

    @Test
    @DisplayName("레코드가 없을 때 좋아요수 감소하면 레코드가 생성되고 -1이 된다")
    fun decrementLikeCount_whenNoRecord_shouldCreateRecordWithNegativeCount() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()

        // when
        postDailyStatsService.decrementLikeCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats).isNotNull
        assertThat(dailyStats?.likeCount).isEqualTo(-1)
    }

    @Test
    @DisplayName("레코드가 있을 때 조회수 증가하면 기존 값에 1이 추가된다")
    fun incrementViewCount_whenRecordExists_shouldIncrementExistingValue() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()
        createDailyStats(post, today, viewCount = 5)

        // when
        postDailyStatsService.incrementViewCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats?.viewCount).isEqualTo(6)
    }

    @Test
    @DisplayName("레코드가 있을 때 좋아요수 증가하면 기존 값에 1이 추가된다")
    fun incrementLikeCount_whenRecordExists_shouldIncrementExistingValue() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()
        createDailyStats(post, today, likeCount = 3)

        // when
        postDailyStatsService.incrementLikeCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats?.likeCount).isEqualTo(4)
    }

    @Test
    @DisplayName("레코드가 있을 때 좋아요수 감소하면 기존 값에서 1이 감소된다")
    fun decrementLikeCount_whenRecordExists_shouldDecrementExistingValue() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()
        createDailyStats(post, today, likeCount = 5)

        // when
        postDailyStatsService.decrementLikeCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats?.likeCount).isEqualTo(4)
    }

    @Test
    @DisplayName("좋아요수가 0일 때 감소하면 -1이 된다")
    fun decrementLikeCount_whenCountIsZero_shouldBecomeNegative() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()
        createDailyStats(post, today, likeCount = 0)

        // when
        postDailyStatsService.decrementLikeCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats?.likeCount).isEqualTo(-1)
    }

    @Test
    @DisplayName("댓글수 증가 시 레코드가 없으면 생성되고 1이 된다")
    fun incrementCommentCount_whenNoRecord_shouldCreateRecordWithCount1() {
        // given
        val user = createAndSaveUser("test@example.com")
        val post = createAndSavePost("테스트 게시글", user)
        val today = DateUtils.today()

        // when
        postDailyStatsService.incrementCommentCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats).isNotNull
        assertThat(dailyStats?.commentCount).isEqualTo(1)
    }

    @Test
    @DisplayName("댓글수 감소 시 레코드가 없으면 생성되고 -1이 된다")
    fun decrementCommentCount_whenNoRecord_shouldCreateRecordWithCountMinus1() {
        // given
        val user = createAndSaveUser("comment-decrement-no-record@example.com")
        val post = createAndSavePost("댓글 감소 테스트 게시글", user)
        val today = DateUtils.today()

        // when
        postDailyStatsService.decrementCommentCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats).isNotNull
        assertThat(dailyStats?.commentCount).isEqualTo(-1)
    }

    @Test
    @DisplayName("댓글수 감소 시 레코드가 있으면 기존 값에서 1이 감소한다")
    fun decrementCommentCount_whenRecordExists_shouldDecrementExistingValue() {
        // given
        val user = createAndSaveUser("comment-decrement-record@example.com")
        val post = createAndSavePost("댓글 감소 테스트 게시글", user)
        val today = DateUtils.today()
        createDailyStats(post, today, commentCount = 3)

        // when
        postDailyStatsService.decrementCommentCount(post.id!!, today)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, today) }
        assertThat(dailyStats?.commentCount).isEqualTo(2)
    }

    @Test
    @DisplayName("댓글수 감소 시 지정한 통계 일자를 기준으로 감소한다")
    fun decrementCommentCount_withSpecificDate_shouldUseProvidedStatDate() {
        // given
        val user = createAndSaveUser("comment-decrement-date@example.com")
        val post = createAndSavePost("댓글 날짜별 감소 테스트 게시글", user)
        val statDate = Date.valueOf("2026-03-01")
        createDailyStats(post, statDate, commentCount = 2)

        // when
        postDailyStatsService.decrementCommentCount(post.id!!, statDate)
        entityManager.flush()
        entityManager.clear()

        // then
        val dailyStats =
            postDailyStatsRepository.findAll()
                .find { it.post.id == post.id && isSameUtcDate(it.statDate, statDate) }
        assertThat(dailyStats?.commentCount).isEqualTo(1)
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

    private fun createDailyStats(
        post: Post,
        statDate: Date,
        viewCount: Long = 0,
        likeCount: Long = 0,
        commentCount: Long = 0,
    ): PostDailyStats {
        val dailyStats =
            PostDailyStats(
                post = post,
                statDate = statDate,
                viewCount = viewCount,
                likeCount = likeCount,
                commentCount = commentCount,
            )
        return postDailyStatsRepository.save(dailyStats)
    }

    private fun isSameUtcDate(
        actual: Date,
        expected: Date,
    ): Boolean = actual.toString() == expected.toString()
}
