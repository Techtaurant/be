package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.enums.LinkStatus
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkViewLogRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@DisplayName("LinkViewLogService 통합 테스트")
@Transactional
class LinkViewLogServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var linkViewLogService: LinkViewLogService

    @Autowired
    private lateinit var linkViewLogRepository: LinkViewLogRepository

    @Autowired
    private lateinit var linkRepository: LinkRepository

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
    @DisplayName("회원이 링크를 조회하면 로그가 저장되고 조회수가 증가한다")
    fun recordView_whenMemberViews_shouldCreateLogAndIncrementViewCount() {
        linkViewLogService.recordView(
            linkId = testLink.id!!,
            userId = normalUser.id!!,
            ipAddress = "127.0.0.1",
            userAgent = "Mozilla/5.0",
        )
        entityManager.flush()
        entityManager.clear()

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        assertThat(updatedLink.viewCount).isEqualTo(1)

        val viewLogs = linkViewLogRepository.findAll()
        assertThat(viewLogs).hasSize(1)
        assertThat(viewLogs.first().ipAddress).isEqualTo("127.0.0.1")
        assertThat(viewLogs.first().userAgent).isEqualTo("Mozilla/5.0")

        val dailyStats = findDailyStats()
        assertThat(dailyStats?.viewCount).isEqualTo(1)
    }

    @Test
    @DisplayName("비회원이 링크를 조회하면 로그가 저장되고 조회수가 증가한다")
    fun recordView_whenGuestViews_shouldCreateLogAndIncrementViewCount() {
        linkViewLogService.recordView(
            linkId = testLink.id!!,
            userId = null,
            ipAddress = "192.168.0.1",
            userAgent = "RestAssured",
        )
        entityManager.flush()
        entityManager.clear()

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        assertThat(updatedLink.viewCount).isEqualTo(1)

        val viewLogs = linkViewLogRepository.findAll()
        assertThat(viewLogs).hasSize(1)
        assertThat(viewLogs.first().user).isNull()
        assertThat(findDailyStats()?.viewCount).isEqualTo(1)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회해도 비회원으로 처리되어 정상 동작한다")
    fun recordView_whenUserNotFoundButNullable_shouldWorkAsGuest() {
        linkViewLogService.recordView(
            linkId = testLink.id!!,
            userId = UUID.randomUUID(),
            ipAddress = "203.0.113.1",
            userAgent = "RestAssured",
        )
        entityManager.flush()
        entityManager.clear()

        val updatedLink = linkRepository.findById(testLink.id!!).orElseThrow()
        assertThat(updatedLink.viewCount).isEqualTo(1)
        assertThat(findDailyStats()?.viewCount).isEqualTo(1)
    }

    @Test
    @DisplayName("존재하지 않는 링크를 조회하면 예외가 발생한다")
    fun recordView_whenLinkNotFound_shouldThrowException() {
        assertThatThrownBy {
            linkViewLogService.recordView(
                linkId = UUID.randomUUID(),
                userId = normalUser.id!!,
                ipAddress = "127.0.0.1",
                userAgent = "Mozilla/5.0",
            )
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(LinkStatus.LINK_NOT_FOUND)
    }

    private fun findDailyStats(): LinkDailyStats? {
        val statDate = DateUtils.today()
        return linkDailyStatsRepository.findAll()
            .find { it.link.id == testLink.id && it.statDate.toString() == statDate.toString() }
    }
}
