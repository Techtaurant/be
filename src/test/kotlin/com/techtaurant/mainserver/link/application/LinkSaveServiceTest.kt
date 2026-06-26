package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.util.DateUtils
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkDailyStats
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkDailyStatsRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
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
import java.time.ZoneOffset
import java.util.UUID

@DisplayName("LinkSaveService 통합 테스트")
@Transactional
class LinkSaveServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var linkSaveService: LinkSaveService

    @Autowired
    private lateinit var linkSourceService: LinkSourceService

    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var userLinkRepository: UserLinkRepository

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
    @DisplayName("링크 저장은 일별 저장수를 증가시키고 중복 저장은 추가 증가시키지 않는다")
    fun save_whenNewRelation_shouldIncrementDailySaveCountOnce() {
        linkSaveService.save(testLink.id!!, normalUser.id!!)
        linkSaveService.save(testLink.id!!, normalUser.id!!)
        entityManager.flush()
        entityManager.clear()

        assertThat(userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, testLink.id!!)).isNotNull()
        assertThat(findDailyStats()?.saveCount).isEqualTo(1)
    }

    @Test
    @DisplayName("다른 저장자가 남아 있으면 저장 취소는 일별 저장수를 감소시키고 링크는 유지된다")
    fun unsave_whenOtherSaversRemain_shouldDecrementDailySaveCountAndKeepLink() {
        registerSource(companyUser, daysAgo = 2)
        linkSaveService.save(testLink.id!!, normalUser.id!!)
        entityManager.flush()
        entityManager.clear()

        linkSaveService.unsave(testLink.id!!, normalUser.id!!)
        entityManager.flush()
        entityManager.clear()

        assertThat(userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, testLink.id!!)).isNull()
        assertThat(findDailyStats()?.saveCount).isEqualTo(0)
        assertThat(linkRepository.findById(testLink.id!!)).isPresent()
    }

    @Test
    @DisplayName("마지막 저장자가 취소하면 링크가 삭제된다")
    fun unsave_whenLastSaver_shouldDeleteLink() {
        linkSaveService.save(testLink.id!!, normalUser.id!!)
        entityManager.flush()
        entityManager.clear()

        linkSaveService.unsave(testLink.id!!, normalUser.id!!)
        entityManager.flush()
        entityManager.clear()

        assertThat(userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, testLink.id!!)).isNull()
        assertThat(linkRepository.findById(testLink.id!!)).isEmpty()
        assertThat(linkDailyStatsRepository.findAll().any { it.link.id == testLink.id }).isFalse()
    }

    @Test
    @DisplayName("첫 등록자가 취소해도 다른 저장자가 있으면 소유권이 다음 저장자로 이전된다")
    fun unsave_whenFirstSourceWithOtherSavers_shouldTransferOwnership() {
        registerSource(normalUser, daysAgo = 2)
        registerSource(companyUser, daysAgo = 1)

        linkSaveService.unsave(testLink.id!!, normalUser.id!!)
        entityManager.flush()
        entityManager.clear()

        assertThat(userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, testLink.id!!)).isNull()
        assertThat(userLinkRepository.findByUserIdAndLinkId(companyUser.id!!, testLink.id!!)).isNotNull()
        assertThat(linkRepository.findById(testLink.id!!)).isPresent()
        assertThat(linkSourceService.getSourceUserId(testLink.id!!)).isEqualTo(companyUser.id)
    }

    @Test
    @DisplayName("일별 통계가 없는 기존 저장을 취소해도 음수 저장수 레코드를 만들지 않는다")
    fun unsave_whenLegacyRelationWithoutDailyStats_shouldNotCreateNegativeDailyStats() {
        registerSource(companyUser, daysAgo = 2)
        val oldStatDate = DateUtils.today().minusDays(1)
        val existingRelation =
            userLinkRepository.saveAndFlush(
                UserLink(
                    user = normalUser,
                    link = testLink,
                ),
            )
        existingRelation.createdAt = oldStatDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        userLinkRepository.saveAndFlush(existingRelation)
        entityManager.clear()

        linkSaveService.unsave(testLink.id!!, normalUser.id!!)
        entityManager.flush()
        entityManager.clear()

        assertThat(userLinkRepository.findByUserIdAndLinkId(normalUser.id!!, testLink.id!!)).isNull()
        assertThat(linkDailyStatsRepository.findAll()).isEmpty()
    }

    private fun registerSource(
        user: User,
        daysAgo: Long,
    ) {
        val relation =
            userLinkRepository.saveAndFlush(
                UserLink(
                    user = user,
                    link = testLink,
                ),
            )
        relation.createdAt = DateUtils.today().minusDays(daysAgo).atStartOfDay(ZoneOffset.UTC).toInstant()
        userLinkRepository.saveAndFlush(relation)
        entityManager.clear()
    }

    private fun findDailyStats(): LinkDailyStats? {
        val statDate = DateUtils.today()
        return linkDailyStatsRepository.findAll()
            .find { it.link.id == testLink.id && it.statDate.toString() == statDate.toString() }
    }
}
