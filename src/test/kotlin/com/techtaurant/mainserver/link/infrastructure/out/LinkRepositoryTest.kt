package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.test.assertFailsWith

@Transactional
@DisplayName("LinkRepository 통합 테스트")
class LinkRepositoryTest : IntegrationTest() {
    @Autowired
    private lateinit var linkRepository: LinkRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var companyUser: User

    @BeforeEach
    fun setUpTestData() {
        companyUser =
            userRepository.save(
                User(
                    name = "토스",
                    email = "toss@example.com",
                    provider = OAuthProvider.SYSTEM,
                    identifier = "company-toss",
                    role = UserRole.COMPANY,
                    profileImageUrl = "https://example.com/toss.png",
                ),
            )
    }

    @Test
    @DisplayName("같은 URL은 전역에서 하나의 링크만 허용한다")
    fun linkUrlShouldBeGloballyUnique() {
        // Given
        val savedLink =
            linkRepository.save(
                Link(
                    title = "첫 번째 링크",
                    url = "https://toss.tech/article/test-link",
                    summary = "요약입니다",
                    publishedAt = Instant.parse("2026-04-25T10:15:30Z"),
                ),
            )

        // When
        val result =
            assertFailsWith<DataIntegrityViolationException> {
                linkRepository.saveAndFlush(
                    Link(
                        title = "두 번째 링크",
                        url = "https://toss.tech/article/test-link",
                        summary = "다른 요약입니다",
                        publishedAt = null,
                    ),
                )
            }

        // Then
        assertThat(result).isNotNull()
        assertThat(savedLink.id).isNotNull()
    }

    @Test
    @DisplayName("URL은 1000자를 초과할 수 있고 요약은 긴 본문을 저장할 수 있다")
    fun linkShouldAllowUrlOverOneThousandCharactersAndLongSummary() {
        // Given
        val longUrl = "https://toss.tech/article/" + "a".repeat(1_100)
        val longSummary = "대규모 게시글 본문 ".repeat(300)

        // When
        val savedLink =
            linkRepository.saveAndFlush(
                Link(
                    title = "긴 링크와 긴 요약",
                    url = longUrl,
                    summary = longSummary,
                ),
            )

        // Then
        assertThat(savedLink.url).hasSizeGreaterThan(1_000)
        assertThat(savedLink.summary).hasSizeGreaterThan(1_000)
    }
}
