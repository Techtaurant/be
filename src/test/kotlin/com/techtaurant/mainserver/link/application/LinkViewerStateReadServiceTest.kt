package com.techtaurant.mainserver.link.application

import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.LinkReadLog
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkReadLogRepository
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class LinkViewerStateReadServiceTest {
    private val linkRepository: LinkRepository = mockk()
    private val userLinkRepository: UserLinkRepository = mockk()
    private val linkReadLogRepository: LinkReadLogRepository = mockk()

    private val linkViewerStateReadService =
        LinkViewerStateReadService(
            linkRepository = linkRepository,
            userLinkRepository = userLinkRepository,
            linkReadLogRepository = linkReadLogRepository,
        )

    @Test
    @DisplayName("로그인 사용자별 저장과 읽음 상태를 링크 ID 순서대로 반환한다")
    fun getLinkViewerStates_returnsSavedAndReadStatesInRequestOrder() {
        // given
        val viewer = createUser("조회자", UserRole.USER)
        val readLink = createLink()
        val savedLink = createLink()
        val missingLinkId = UUID.randomUUID()

        every { linkRepository.findAllById(listOf(readLink.id!!, savedLink.id!!, missingLinkId)) } returns listOf(savedLink, readLink)
        every { userLinkRepository.findSavedByUserIdAndLinkIdIn(viewer.id!!, listOf(readLink.id!!, savedLink.id!!)) } returns
            listOf(UserLink(user = viewer, link = savedLink))
        every { linkReadLogRepository.findByUserIdAndLinkIdIn(viewer.id!!, listOf(readLink.id!!, savedLink.id!!)) } returns
            listOf(LinkReadLog(user = viewer, link = readLink))

        // when
        val result =
            linkViewerStateReadService.getLinkViewerStates(
                userId = viewer.id!!,
                linkIds = listOf(readLink.id!!, savedLink.id!!, readLink.id!!, missingLinkId),
            )

        // then
        assertThat(result.map { it.linkId }).containsExactly(readLink.id, savedLink.id)
        assertThat(result[0].isSaved).isFalse()
        assertThat(result[0].isRead).isTrue()
        assertThat(result[1].isSaved).isTrue()
        assertThat(result[1].isRead).isFalse()
    }

    @Test
    @DisplayName("링크 ID 목록이 비어 있으면 빈 목록을 반환한다")
    fun getLinkViewerStates_emptyLinkIds_returnsEmptyList() {
        // given
        val viewerId = UUID.randomUUID()

        // when
        val result = linkViewerStateReadService.getLinkViewerStates(userId = viewerId, linkIds = emptyList())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("요청한 링크가 존재하지 않으면 빈 목록을 반환한다")
    fun getLinkViewerStates_missingLinks_returnsEmptyList() {
        // given
        val viewerId = UUID.randomUUID()
        val missingLinkId = UUID.randomUUID()
        every { linkRepository.findAllById(listOf(missingLinkId)) } returns emptyList()

        // when
        val result = linkViewerStateReadService.getLinkViewerStates(userId = viewerId, linkIds = listOf(missingLinkId))

        // then
        assertThat(result).isEmpty()
    }

    private fun createUser(
        name: String,
        role: UserRole,
    ): User =
        User(
            name = name,
            email = "${UUID.randomUUID()}@example.com",
            provider = OAuthProvider.GOOGLE,
            identifier = UUID.randomUUID().toString(),
            role = role,
            profileImageUrl = "https://example.com/profile.jpg",
        ).apply { id = UUID.randomUUID() }

    private fun createLink(): Link =
        Link(
            title = "링크",
            url = "https://example.com/${UUID.randomUUID()}",
            summary = "요약",
        ).apply { id = UUID.randomUUID() }
}
