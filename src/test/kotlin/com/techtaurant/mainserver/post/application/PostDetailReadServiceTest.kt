package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostLikeLog
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class PostDetailReadServiceTest {
    private val postRepository: PostRepository = mockk()
    private val postViewLogService: PostViewLogService = mockk()
    private val postLikeLogRepository: PostLikeLogRepository = mockk()
    private val postReadLogRepository: PostReadLogRepository = mockk()
    private val attachmentService: AttachmentService = mockk()

    private val postDetailReadService =
        PostDetailReadService(
            postRepository = postRepository,
            postViewLogService = postViewLogService,
            postLikeLogRepository = postLikeLogRepository,
            postReadLogRepository = postReadLogRepository,
            attachmentService = attachmentService,
        )

    private lateinit var author: User

    @BeforeEach
    fun setUp() {
        author =
            User(
                name = "작성자",
                email = "writer@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "writer-id",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/profile.jpg",
            ).apply { id = UUID.randomUUID() }

        every { postViewLogService.recordView(any(), any(), any(), any()) } just runs
        every { attachmentService.generatePresignedDownloadUrlMapByReference(any(), any()) } returns emptyMap()
    }

    @Nested
    @DisplayName("getPostDetail")
    inner class GetPostDetail {
        @Test
        @DisplayName("연결된 attachmentId를 presigned URL로 치환한다")
        fun getPostDetail_attachmentReferences_replacesContentTargets() {
            // given
            val postId = UUID.randomUUID()
            val viewerId = UUID.randomUUID()
            val attachmentId = UUID.randomUUID()
            val content = "<img src=\"$attachmentId\" />"
            val post =
                Post(
                    title = "게시물",
                    content = content,
                    author = author,
                    status = PostStatusEnum.PUBLISHED,
                ).apply { id = postId }

            every { postRepository.findVisiblePostDetailById(postId, viewerId) } returns post
            every { postLikeLogRepository.findByPostIdAndUserId(postId, viewerId) } returns null
            every { postReadLogRepository.existsByPostIdAndUserId(postId, viewerId) } returns true
            every {
                attachmentService.generatePresignedDownloadUrlMapByReference(
                    postId,
                    AttachmentReferenceType.POST,
                )
            } returns mapOf(attachmentId to "https://cdn.example.com/attachment.png")

            // when
            val result = postDetailReadService.getPostDetail(postId, viewerId, "127.0.0.1", "JUnit")

            // then
            assertThat(result.likeStatus).isEqualTo(LikeStatus.NONE)
            assertThat(result.isRead).isTrue()
            assertThat(result.content).contains("https://cdn.example.com/attachment.png")
            verify {
                postViewLogService.recordView(postId, viewerId, "127.0.0.1", "JUnit")
            }
        }

        @Test
        @DisplayName("비로그인 사용자는 읽음 여부를 false로 반환한다")
        fun getPostDetail_anonymousUser_returnsUnread() {
            // given
            val postId = UUID.randomUUID()
            val post =
                Post(
                    title = "게시물",
                    content = "본문",
                    author = author,
                    status = PostStatusEnum.PUBLISHED,
                ).apply { id = postId }

            every { postRepository.findVisiblePostDetailById(postId, null) } returns post

            // when
            val result = postDetailReadService.getPostDetail(postId, null, null, null)

            // then
            assertThat(result.isRead).isFalse()
        }

        @Test
        @DisplayName("좋아요 로그가 있으면 likeStatus에 반영한다")
        fun getPostDetail_likedPost_returnsLikeStatus() {
            // given
            val postId = UUID.randomUUID()
            val viewerId = UUID.randomUUID()
            val post =
                Post(
                    title = "게시물",
                    content = "본문",
                    author = author,
                    status = PostStatusEnum.PUBLISHED,
                ).apply { id = postId }
            val likeLog = PostLikeLog(post = post, user = author, isLiked = true)

            every { postRepository.findVisiblePostDetailById(postId, viewerId) } returns post
            every { postLikeLogRepository.findByPostIdAndUserId(postId, viewerId) } returns likeLog
            every { postReadLogRepository.existsByPostIdAndUserId(postId, viewerId) } returns false

            // when
            val result = postDetailReadService.getPostDetail(postId, viewerId, null, null)

            // then
            assertThat(result.likeStatus).isEqualTo(LikeStatus.LIKE)
        }
    }
}
