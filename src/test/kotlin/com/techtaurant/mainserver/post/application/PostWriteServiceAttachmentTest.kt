package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.common.lock.DistributedLock
import com.techtaurant.mainserver.post.dto.CreatePostRequest
import com.techtaurant.mainserver.post.dto.UpdatePostRequest
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.post.infrastructure.out.TagRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class PostWriteServiceAttachmentTest {
    private val postRepository: PostRepository = mockk()
    private val categoryRepository: CategoryRepository = mockk()
    private val tagRepository: TagRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val distributedLock: DistributedLock = mockk()
    private val attachmentService: AttachmentService = mockk()

    private val postWriteService =
        PostWriteService(
            postRepository = postRepository,
            categoryRepository = categoryRepository,
            tagRepository = tagRepository,
            userRepository = userRepository,
            distributedLock = distributedLock,
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

        every { userRepository.findById(author.id!!) } returns Optional.of(author)
        every { attachmentService.confirmAttachmentsByIds(any(), any(), any()) } just runs
        every { attachmentService.deleteOrphanedAttachmentsByIds(any(), any(), any()) } just runs
        every { attachmentService.deleteAttachmentsByReference(any(), any()) } just runs

        every { postRepository.save(any()) } answers {
            firstArg<Post>().apply {
                if (id == null) {
                    id = UUID.randomUUID()
                }
            }
        }
        every { postRepository.delete(any()) } just runs
    }

    @Nested
    @DisplayName("createPost")
    inner class CreatePost {
        @Test
        @DisplayName("발행 게시물 본문의 attachmentId를 확정하고 본문은 그대로 유지한다")
        fun createPost_publishedPost_confirmsAttachmentIdsWithoutRewritingContent() {
            // given
            val attachmentId = UUID.randomUUID()
            val request =
                CreatePostRequest(
                    title = "게시물",
                    content = "<p>본문</p><img src=\"$attachmentId\" />",
                    attachmentIds = listOf(attachmentId),
                    status = PostStatusEnum.PUBLISHED,
                )

            // when
            val response = postWriteService.createPost(author.id!!, request)

            // then
            verify {
                attachmentService.confirmAttachmentsByIds(
                    response.id,
                    AttachmentReferenceType.POST,
                    listOf(attachmentId),
                )
            }
            assertThat(response.content).contains(attachmentId.toString())
        }
    }

    @Nested
    @DisplayName("updatePost")
    inner class UpdatePost {
        @Test
        @DisplayName("요청으로 받은 attachmentId 목록 기준으로 orphan 첨부를 정리한다")
        fun updatePost_attachmentIdsRequest_keepsRequestedAttachments() {
            // given
            val postId = UUID.randomUUID()
            val newAttachmentId = UUID.randomUUID()
            val post =
                Post(
                    title = "기존 제목",
                    content = "기존 본문",
                    author = author,
                    status = PostStatusEnum.PUBLISHED,
                ).apply { id = postId }

            every { postRepository.findPostByIdWithAuthor(postId) } returns post

            val request =
                UpdatePostRequest(
                    content = "<img src=\"$newAttachmentId\" />",
                    attachmentIds = listOf(newAttachmentId),
                    status = PostStatusEnum.PUBLISHED,
                )

            // when
            val response = postWriteService.updatePost(postId, request, author.id!!)

            // then
            verify {
                attachmentService.confirmAttachmentsByIds(
                    postId,
                    AttachmentReferenceType.POST,
                    listOf(newAttachmentId),
                )
            }
            verify {
                attachmentService.deleteOrphanedAttachmentsByIds(
                    postId,
                    AttachmentReferenceType.POST,
                    listOf(newAttachmentId),
                )
            }
            assertThat(response.content).contains(newAttachmentId.toString())
        }
    }

    @Nested
    @DisplayName("deletePost")
    inner class DeletePost {
        @Test
        @DisplayName("게시물 삭제 시 연관 첨부를 먼저 정리한 뒤 게시물을 삭제한다")
        fun deletePost_existingPost_deletesAttachmentsBeforePost() {
            // given
            val postId = UUID.randomUUID()
            val post =
                Post(
                    title = "삭제 대상",
                    content = "본문",
                    author = author,
                    status = PostStatusEnum.PUBLISHED,
                ).apply { id = postId }
            every { postRepository.findPostByIdWithAuthor(postId) } returns post

            // when
            postWriteService.deletePost(postId, author.id!!)

            // then
            verifyOrder {
                postRepository.findPostByIdWithAuthor(postId)
                attachmentService.deleteAttachmentsByReference(postId, AttachmentReferenceType.POST)
                postRepository.delete(post)
            }
        }
    }
}
