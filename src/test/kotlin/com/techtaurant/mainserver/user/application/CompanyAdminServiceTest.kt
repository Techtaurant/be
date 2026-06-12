package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.link.entity.Link
import com.techtaurant.mainserver.link.entity.UserLink
import com.techtaurant.mainserver.link.infrastructure.out.LinkRepository
import com.techtaurant.mainserver.link.infrastructure.out.UserLinkRepository
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import com.techtaurant.mainserver.user.infrastructure.out.UserTokenRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional
import java.util.UUID

@DisplayName("CompanyAdminService 테스트")
class CompanyAdminServiceTest {
    private val userRepository: UserRepository = mockk()
    private val userTokenRepository: UserTokenRepository = mockk(relaxed = true)
    private val jwtTokenProvider: JwtTokenProvider = mockk(relaxed = true)
    private val attachmentService: AttachmentService = mockk()
    private val userResponseAssembler: UserResponseAssembler = mockk(relaxed = true)
    private val postRepository: PostRepository = mockk()
    private val userLinkRepository: UserLinkRepository = mockk()
    private val linkRepository: LinkRepository = mockk()

    private val companyAdminService =
        CompanyAdminService(
            userRepository = userRepository,
            userTokenRepository = userTokenRepository,
            jwtTokenProvider = jwtTokenProvider,
            attachmentService = attachmentService,
            userResponseAssembler = userResponseAssembler,
            postRepository = postRepository,
            userLinkRepository = userLinkRepository,
            linkRepository = linkRepository,
        )

    private lateinit var companyUser: User

    @BeforeEach
    fun setUp() {
        companyUser = createUser(UserRole.COMPANY)

        every { attachmentService.deleteAttachmentsByReference(any(), any()) } just runs
        every { userRepository.delete(any()) } just runs
        every { userLinkRepository.deleteAllByLink(any()) } returns 0L
        every { linkRepository.delete(any()) } just runs
    }

    @Test
    @DisplayName("admin company 삭제는 USER/POST attachment를 정리하고 회사 링크와 해당 링크의 모든 user_links 관계를 삭제한다")
    fun deleteCompany_deletesAttachmentsLinksAndCompanyUser() {
        val post =
            Post(
                title = "회사 게시물",
                content = "본문",
                author = companyUser,
                thumbnailImage = UUID.randomUUID(),
            ).apply { id = UUID.randomUUID() }
        val link =
            Link(
                title = "회사 링크",
                url = "https://example.com/${UUID.randomUUID()}",
                summary = "요약",
                publishedAt = Instant.now(),
            ).apply { id = UUID.randomUUID() }
        val userLink = UserLink(companyUser, link).apply { id = UUID.randomUUID() }

        every { userRepository.findById(companyUser.id!!) } returns Optional.of(companyUser)
        every { postRepository.findAllByAuthorId(companyUser.id!!) } returns listOf(post)
        every { postRepository.deleteAll(listOf(post)) } just runs
        every { userLinkRepository.findAllByUserId(companyUser.id!!) } returns listOf(userLink)
        every { userLinkRepository.deleteAllByLink(link) } returns 2L

        companyAdminService.deleteCompany(companyUser.id!!)

        assertThat(post.thumbnailImage).isNull()
        verifyOrder {
            userRepository.findById(companyUser.id!!)
            postRepository.findAllByAuthorId(companyUser.id!!)
            attachmentService.deleteAttachmentsByReference(companyUser.id!!, AttachmentReferenceType.USER)
            attachmentService.deleteAttachmentsByReference(post.id!!, AttachmentReferenceType.POST)
            postRepository.deleteAll(listOf(post))
            userLinkRepository.findAllByUserId(companyUser.id!!)
            userLinkRepository.deleteAllByLink(link)
            linkRepository.delete(link)
            userRepository.delete(companyUser)
        }
    }

    @Test
    @DisplayName("admin company 삭제 전용 동작이므로 COMPANY가 아닌 사용자는 삭제하지 않는다")
    fun deleteCompany_nonCompanyUser_throwsCompanyNotFound() {
        val normalUser = createUser(UserRole.USER)
        every { userRepository.findById(normalUser.id!!) } returns Optional.of(normalUser)

        assertThatThrownBy {
            companyAdminService.deleteCompany(normalUser.id!!)
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(UserStatus.COMPANY_NOT_FOUND)

        verify(exactly = 0) { postRepository.findAllByAuthorId(any()) }
        verify(exactly = 0) { attachmentService.deleteAttachmentsByReference(any(), any()) }
        verify(exactly = 0) { userLinkRepository.findAllByUserId(any()) }
        verify(exactly = 0) { userRepository.delete(any()) }
    }

    private fun createUser(role: UserRole): User =
        User(
            name = "사용자-${UUID.randomUUID()}",
            email = "user-${UUID.randomUUID()}@example.com",
            provider = OAuthProvider.GOOGLE,
            identifier = "identifier-${UUID.randomUUID()}",
            role = role,
            profileImageUrl = "https://example.com/profile.jpg",
        ).apply { id = UUID.randomUUID() }
}
