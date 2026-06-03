package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkLikeLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("링크 상호작용 동시성 통합 테스트")
class LinkInteractionConcurrencyTest : IntegrationTest() {
    @Autowired
    private lateinit var linkViewLogService: LinkViewLogService

    @Autowired
    private lateinit var linkLikeLogService: LinkLikeLogService

    @Autowired
    private lateinit var linkSaveService: LinkSaveService

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var linkLikeLogRepository: LinkLikeLogRepository

    @Autowired
    private lateinit var userLinkRepository: UserLinkRepository

    @Autowired
    private lateinit var linkDailyStatsRepository: LinkDailyStatsRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var companyUser: User
    private lateinit var normalUser: User
    private lateinit var testLink: Link

    @BeforeEach
    fun setUpTestData() {
        companyUser =
            userRepository.saveAndFlush(
                User(
                    name = "토스",
                    email = "company-${UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.SYSTEM,
                    identifier = "company-${UUID.randomUUID()}",
                    role = UserRole.COMPANY,
                    profileImageUrl = "https://example.com/company.png",
                ),
            )

        normalUser =
            userRepository.saveAndFlush(
                User(
                    name = "일반사용자",
                    email = "user-${UUID.randomUUID()}@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "user-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/user.png",
                ),
            )

        testLink =
            linkRepository.saveAndFlush(
                Link(
                    title = "테스트 링크",
                    url = "https://toss.tech/article/${UUID.randomUUID()}",
                    summary = "테스트 링크 요약입니다.",
                ),
            )
    }

    @Test
    @DisplayName("동시 최초 조회는 일별 통계 레코드를 하나만 만들고 모든 조회를 집계한다")
    fun recordView_whenConcurrentFirstViews_shouldCreateOneDailyStatsAndCountBoth() {
        runConcurrently {
            linkViewLogService.recordView(
                linkId = testLink.id!!,
                userId = null,
                ipAddress = "127.0.0.1",
                userAgent = "test-agent",
            )
        }

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        val dailyStats = findDailyStats()

        assertThat(updatedLink.viewCount).isEqualTo(2)
        assertThat(dailyStats?.viewCount).isEqualTo(2)
        assertThat(linkDailyStatsRepository.findAll()).hasSize(1)
    }

    @Test
    @DisplayName("동시 좋아요 전이는 한 번만 집계된다")
    fun recordLike_whenConcurrentTransitions_shouldApplyAggregateDeltaOnce() {
        linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.DISLIKE)

        runConcurrently {
            linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.LIKE)
        }

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        val likeLog = linkLikeLogRepository.findByLinkIdAndUserId(testLink.id!!, normalUser.id!!)
        val dailyStats = findDailyStats()

        assertThat(updatedLink.likeCount).isEqualTo(1)
        assertThat(likeLog?.isLiked).isTrue()
        assertThat(dailyStats?.likeCount).isEqualTo(1)
    }

    @Test
    @DisplayName("동시 최초 좋아요는 한 번만 생성하고 집계한다")
    fun recordLike_whenConcurrentFirstLikes_shouldInsertAndCountOnce() {
        runConcurrently {
            linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.LIKE)
        }

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        val likeLog = linkLikeLogRepository.findByLinkIdAndUserId(testLink.id!!, normalUser.id!!)
        val dailyStats = findDailyStats()

        assertThat(updatedLink.likeCount).isEqualTo(1)
        assertThat(likeLog?.isLiked).isTrue()
        assertThat(dailyStats?.likeCount).isEqualTo(1)
        assertThat(linkLikeLogRepository.findByUserIdAndLinkIdIn(normalUser.id!!, listOf(testLink.id!!))).hasSize(1)
    }

    @Test
    @DisplayName("동시 최초 싫어요는 한 번만 생성하고 집계한다")
    fun recordLike_whenConcurrentFirstDislikes_shouldInsertAndCountOnce() {
        runConcurrently {
            linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.DISLIKE)
        }

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        val likeLog = linkLikeLogRepository.findByLinkIdAndUserId(testLink.id!!, normalUser.id!!)
        val dailyStats = findDailyStats()

        assertThat(updatedLink.likeCount).isEqualTo(-1)
        assertThat(likeLog?.isLiked).isFalse()
        assertThat(dailyStats?.likeCount).isEqualTo(-1)
        assertThat(linkLikeLogRepository.findByUserIdAndLinkIdIn(normalUser.id!!, listOf(testLink.id!!))).hasSize(1)
    }

    @Test
    @DisplayName("동시 최초 저장은 한 번만 생성하고 저장수를 집계한다")
    fun save_whenConcurrentFirstRequests_shouldInsertAndCountOnce() {
        runConcurrently {
            linkSaveService.save(testLink.id!!, normalUser.id!!)
        }

        val userLink = userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, testLink.id!!)
        val dailyStats = findDailyStats()

        assertThat(userLink).isNotNull
        assertThat(userLinkRepository.findByUserIdAndLinkIdIn(normalUser.id!!, listOf(testLink.id!!))).hasSize(1)
        assertThat(dailyStats?.saveCount).isEqualTo(1)
    }

    @Test
    @DisplayName("동시 저장 취소는 한 번만 저장수를 감소시킨다")
    fun unsave_whenConcurrentRequests_shouldDecrementDailySaveCountOnce() {
        linkSaveService.save(testLink.id!!, normalUser.id!!)

        runConcurrently {
            linkSaveService.unsave(testLink.id!!, normalUser.id!!)
        }

        val userLink = userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, testLink.id!!)
        val dailyStats = findDailyStats()

        assertThat(userLink).isNull()
        assertThat(dailyStats?.saveCount).isEqualTo(0)
    }

    private fun findDailyStats(statDate: LocalDate = DateUtils.today()): LinkDailyStats? {
        return linkDailyStatsRepository.findAll()
            .find { it.link.id == testLink.id && it.statDate.toString() == statDate.toString() }
    }

    private fun runConcurrently(operation: () -> Unit) {
        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val failures = ConcurrentLinkedQueue<Throwable>()

        repeat(2) {
            executor.execute {
                try {
                    ready.countDown()
                    start.await()
                    operation()
                } catch (throwable: Throwable) {
                    failures.add(throwable)
                } finally {
                    done.countDown()
                }
            }
        }

        try {
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue()
            assertThat(failures).isEmpty()
        } finally {
            executor.shutdownNow()
        }
    }
}
