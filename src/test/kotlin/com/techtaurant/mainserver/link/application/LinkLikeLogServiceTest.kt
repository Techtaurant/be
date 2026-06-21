package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.entity.LinkLikeLog
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkLikeLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@DisplayName("LinkLikeLogService 통합 테스트")
@Transactional
class LinkLikeLogServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var linkLikeLogService: LinkLikeLogService

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var linkLikeLogRepository: LinkLikeLogRepository

    @Autowired
    private lateinit var linkDailyStatsRepository: LinkDailyStatsRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var companyUser: User
    private lateinit var normalUser: User
    private lateinit var testLink: Link

    @BeforeEach
    fun setUpTestData() {
        companyUser =
            userRepository.save(
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
            userRepository.save(
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
            linkRepository.save(
                Link(
                    title = "테스트 링크",
                    url = "https://toss.tech/article/${UUID.randomUUID()}",
                    summary = "테스트 링크 요약입니다.",
                ),
            )
    }

    @Test
    @DisplayName("중립 상태에서 좋아요를 기록하면 likeCount가 1 증가한다")
    fun recordLike_fromNeutralToLike_shouldIncrementLikeCount() {
        linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.LIKE)
        entityManager.flush()
        entityManager.clear()

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        assertThat(updatedLink.likeCount).isEqualTo(1)

        val log = linkLikeLogRepository.findByLinkIdAndUserId(testLink.id!!, normalUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.isLiked).isTrue()

        val dailyStats = findDailyStats()
        assertThat(dailyStats?.likeCount).isEqualTo(1)
    }

    @Test
    @DisplayName("좋아요 상태에서 취소하면 likeCount가 1 감소하고 로그가 삭제된다")
    fun recordLike_fromLikeToNone_shouldDecrementLikeCountAndDeleteLog() {
        linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.LIKE)
        entityManager.flush()
        entityManager.clear()

        linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.NONE)
        entityManager.flush()
        entityManager.clear()

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        assertThat(updatedLink.likeCount).isEqualTo(0)
        assertThat(linkLikeLogRepository.findByLinkIdAndUserId(testLink.id!!, normalUser.id!!)).isNull()
        assertThat(findDailyStats()?.likeCount).isEqualTo(0)
    }

    @Test
    @DisplayName("싫어요 상태에서 좋아요로 변경하면 likeCount가 2 증가한다")
    fun recordLike_fromDislikeToLike_shouldIncrementLikeCountByTwo() {
        linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.DISLIKE)
        entityManager.flush()
        entityManager.clear()

        linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.LIKE)
        entityManager.flush()
        entityManager.clear()

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        assertThat(updatedLink.likeCount).isEqualTo(1)
        assertThat(linkLikeLogRepository.findByLinkIdAndUserId(testLink.id!!, normalUser.id!!)?.isLiked).isTrue()
        assertThat(findDailyStats()?.likeCount).isEqualTo(1)
    }

    @Test
    @DisplayName("과거 좋아요를 오늘 취소하면 오늘 일별 좋아요수만 감소한다")
    fun recordLike_fromPastLikeToNone_shouldRecordDailyStatsOnEventDate() {
        val today = DateUtils.today()
        val oldStatDate = today.minusDays(1)
        testLink.likeCount = 1
        linkRepository.saveAndFlush(testLink)
        linkDailyStatsRepository.saveAndFlush(LinkDailyStats(link = testLink, statDate = oldStatDate, likeCount = 1))
        val existingLog =
            linkLikeLogRepository.saveAndFlush(
                LinkLikeLog(
                    link = testLink,
                    user = normalUser,
                    isLiked = true,
                ),
            )
        existingLog.createdAt = oldStatDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        linkLikeLogRepository.saveAndFlush(existingLog)
        entityManager.clear()

        linkLikeLogService.recordLike(testLink.id!!, normalUser.id!!, LikeStatus.NONE)
        entityManager.flush()
        entityManager.clear()

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        assertThat(updatedLink.likeCount).isEqualTo(0)
        assertThat(linkLikeLogRepository.findByLinkIdAndUserId(testLink.id!!, normalUser.id!!)).isNull()
        assertThat(findDailyStats(oldStatDate)?.likeCount).isEqualTo(1)
        assertThat(findDailyStats(today)?.likeCount).isEqualTo(-1)
    }

    @Test
    @DisplayName("존재하지 않는 링크에 좋아요를 기록하면 예외가 발생한다")
    fun recordLike_withNonExistentLink_shouldThrowException() {
        assertThatThrownBy {
            linkLikeLogService.recordLike(UUID.randomUUID(), normalUser.id!!, LikeStatus.LIKE)
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(LinkStatus.LINK_NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 좋아요를 기록하면 예외가 발생한다")
    fun recordLike_withNonExistentUser_shouldThrowException() {
        assertThatThrownBy {
            linkLikeLogService.recordLike(testLink.id!!, UUID.randomUUID(), LikeStatus.LIKE)
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(UserStatus.ID_NOT_FOUND)
    }

    private fun findDailyStats(statDate: LocalDate = DateUtils.today()): LinkDailyStats? {
        return linkDailyStatsRepository.findAll()
            .find { it.link.id == testLink.id && it.statDate.toString() == statDate.toString() }
    }
}
