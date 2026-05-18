package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
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
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.util.UUID

@DisplayName("LinkDailyStatsService 통합 테스트")
@Transactional
class LinkDailyStatsServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var linkDailyStatsService: LinkDailyStatsService

    @Autowired
    private lateinit var linkDailyStatsRepository: LinkDailyStatsRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var companyUser: User
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

        testLink =
            linkRepository.save(
                Link(
                    title = "테스트 링크",
                    url = "https://toss.tech/article/${UUID.randomUUID()}",
                    summary = "테스트 링크 요약입니다.",
                    sourceCompanyUser = companyUser,
                ),
            )
    }

    @Test
    @DisplayName("일별 통계 레코드가 없으면 생성 후 조회수와 저장수를 증가시킨다")
    fun incrementStats_whenDailyStatsNotExists_shouldCreateAndUpdate() {
        val statDate = Date.valueOf("2026-05-18")

        linkDailyStatsService.incrementViewCount(testLink.id!!, statDate)
        linkDailyStatsService.incrementSaveCount(testLink.id!!, statDate)
        entityManager.flush()
        entityManager.clear()

        val dailyStats = linkDailyStatsRepository.findAll().single()
        assertThat(dailyStats.link.id).isEqualTo(testLink.id)
        assertThat(dailyStats.statDate.toString()).isEqualTo(statDate.toString())
        assertThat(dailyStats.viewCount).isEqualTo(1)
        assertThat(dailyStats.likeCount).isEqualTo(0)
        assertThat(dailyStats.saveCount).isEqualTo(1)
    }

    @Test
    @DisplayName("좋아요 감소는 레코드가 없으면 생성 후 음수 값을 기록할 수 있다")
    fun decrementLikeCount_whenDailyStatsNotExists_shouldCreateAndAllowNegativeCount() {
        val statDate = Date.valueOf("2026-05-18")

        linkDailyStatsService.decrementLikeCount(testLink.id!!, statDate)
        entityManager.flush()
        entityManager.clear()

        val dailyStats = linkDailyStatsRepository.findAll().single()
        assertThat(dailyStats.likeCount).isEqualTo(-1)
        assertThat(dailyStats.saveCount).isEqualTo(0)
    }

    @Test
    @DisplayName("저장수 감소는 레코드가 없으면 생성하지 않고 기존 레코드가 있으면 감소한다")
    fun decrementSaveCount_whenDailyStatsNotExists_shouldNotCreateDailyStats() {
        val statDate = Date.valueOf("2026-05-18")

        linkDailyStatsService.decrementSaveCount(testLink.id!!, statDate)
        entityManager.flush()
        entityManager.clear()

        assertThat(linkDailyStatsRepository.findAll()).isEmpty()

        linkDailyStatsService.incrementSaveCount(testLink.id!!, statDate)
        linkDailyStatsService.decrementSaveCount(testLink.id!!, statDate)
        entityManager.flush()
        entityManager.clear()

        val dailyStats = linkDailyStatsRepository.findAll().single()
        assertThat(dailyStats.saveCount).isEqualTo(0)
    }
}
