package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostReadLog
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class PostListReadServiceTest {
    private val postRepository: PostRepository = mockk()
    private val postReadLogRepository: PostReadLogRepository = mockk()
    private val attachmentService: AttachmentService = mockk()
    private val defaultThumbnailUrl = "/static/images/post-thumbnail.png"
    private val baseUrl = "http://localhost:8080"

    private val postListReadService =
        PostListReadService(
            postRepository = postRepository,
            postReadLogRepository = postReadLogRepository,
            attachmentService = attachmentService,
            defaultThumbnailUrl = defaultThumbnailUrl,
            baseUrl = baseUrl,
        )

    private lateinit var testUser: User
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        testUser =
            User(
                name = "테스트 사용자",
                email = "test@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "test-id",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/profile.jpg",
            ).apply { id = UUID.randomUUID() }

        otherUser =
            User(
                name = "다른 사용자",
                email = "other@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "other-id",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/other-profile.jpg",
            ).apply { id = UUID.randomUUID() }
    }

    private fun createPost(
        author: User,
        status: PostStatusEnum = PostStatusEnum.PUBLISHED,
    ): Post =
        Post(
            title = "테스트 게시물",
            content = "테스트 내용",
            author = author,
            status = status,
        ).apply { id = UUID.randomUUID() }

    @Nested
    @DisplayName("getPosts")
    inner class GetPosts {
        @BeforeEach
        fun setUp() {
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(any(), AttachmentReferenceType.POST)
            } returns emptyMap()
        }

        @Test
        @DisplayName("로그인 사용자 조회 시 visibleToUserId에 현재 사용자 ID를 전달한다")
        fun getPosts_loggedInUser_passesVisibleToUserId() {
            // given
            val posts = listOf(createPost(testUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = testUser.id!!,
                    viewerId = testUser.id!!,
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            postListReadService.getPosts(cursor = null, size = 20, currentUserId = testUser.id!!)

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = testUser.id!!,
                    viewerId = testUser.id!!,
                )
            }
        }

        @Test
        @DisplayName("비로그인 사용자 조회 시 visibleToUserId에 null을 전달한다")
        fun getPosts_anonymousUser_passesNullVisibleToUserId() {
            // given
            val posts = listOf(createPost(otherUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                    viewerId = null,
                )
            } returns posts

            // when
            postListReadService.getPosts(cursor = null, size = 20, currentUserId = null)

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                    viewerId = null,
                )
            }
        }
    }

    @Nested
    @DisplayName("getPosts (authorId 필터)")
    inner class GetPostsWithAuthorId {
        @BeforeEach
        fun setUp() {
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(any(), AttachmentReferenceType.POST)
            } returns emptyMap()
        }

        @Test
        @DisplayName("본인 조회 시 모든 상태(DRAFT, PUBLISHED, PRIVATE)로 Repository를 호출한다")
        fun getPosts_ownPosts_queriesAllStatuses() {
            // given
            val posts = listOf(createPost(testUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = testUser.id!!,
                    statuses = PostStatusEnum.entries,
                    categoryId = null,
                    viewerId = testUser.id!!,
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            postListReadService.getPosts(
                cursor = null,
                size = 20,
                currentUserId = testUser.id!!,
                authorId = testUser.id!!,
            )

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = testUser.id!!,
                    statuses = PostStatusEnum.entries,
                    categoryId = null,
                    viewerId = testUser.id!!,
                )
            }
        }

        @Test
        @DisplayName("타인 조회 시 PUBLISHED만으로 Repository를 호출한다")
        fun getPosts_otherUserPosts_queriesPublishedOnly() {
            // given
            val posts = listOf(createPost(otherUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = otherUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = null,
                    viewerId = testUser.id!!,
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            postListReadService.getPosts(
                cursor = null,
                size = 20,
                currentUserId = testUser.id!!,
                authorId = otherUser.id!!,
            )

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = otherUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = null,
                    viewerId = testUser.id!!,
                )
            }
        }

        @Test
        @DisplayName("비로그인 사용자 조회 시 PUBLISHED만으로 Repository를 호출하고 isRead는 항상 false이다")
        fun getPosts_anonymousUser_queriesPublishedOnly() {
            // given
            val posts = listOf(createPost(otherUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = otherUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = null,
                    viewerId = null,
                )
            } returns posts

            // when
            val result =
                postListReadService.getPosts(
                    cursor = null,
                    size = 20,
                    currentUserId = null,
                    authorId = otherUser.id!!,
                )

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = otherUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = null,
                    viewerId = null,
                )
            }
            assertThat(result.content).allSatisfy { assertThat(it.isRead).isFalse() }
        }

        @Test
        @DisplayName("잘못된 커서 전달 시 빈 응답을 반환한다")
        fun getPosts_invalidCursor_returnsEmptyResponse() {
            // given & when
            val result =
                postListReadService.getPosts(
                    cursor = "invalid-cursor-string",
                    size = 20,
                    currentUserId = testUser.id!!,
                    authorId = testUser.id!!,
                )

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.nextCursor).isNull()
            assertThat(result.hasNext).isFalse()
            assertThat(result.size).isEqualTo(0)
        }

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext가 true이고 nextCursor가 반환된다")
        fun getPosts_hasMorePosts_returnsHasNextTrue() {
            // given
            val posts = (1..3).map { createPost(testUser) }
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 3,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = testUser.id!!,
                    statuses = PostStatusEnum.entries,
                    categoryId = null,
                    viewerId = testUser.id!!,
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            val result =
                postListReadService.getPosts(
                    cursor = null,
                    size = 2,
                    currentUserId = testUser.id!!,
                    authorId = testUser.id!!,
                )

            // then
            assertThat(result.hasNext).isTrue()
            assertThat(result.nextCursor).isNotNull()
            assertThat(result.content).hasSize(2)
        }

        @Test
        @DisplayName("다음 페이지가 없으면 hasNext가 false이고 nextCursor가 null이다")
        fun getPosts_noMorePosts_returnsHasNextFalse() {
            // given
            val posts = listOf(createPost(testUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = testUser.id!!,
                    statuses = PostStatusEnum.entries,
                    categoryId = null,
                    viewerId = testUser.id!!,
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            val result =
                postListReadService.getPosts(
                    cursor = null,
                    size = 20,
                    currentUserId = testUser.id!!,
                    authorId = testUser.id!!,
                )

            // then
            assertThat(result.hasNext).isFalse()
            assertThat(result.nextCursor).isNull()
            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("로그인 사용자가 조회 시 읽음 기록이 반영된다")
        fun getPosts_loggedInUser_appliesReadStatus() {
            // given
            val post1 = createPost(otherUser)
            val post2 = createPost(otherUser)
            val posts = listOf(post1, post2)
            val readLog = PostReadLog(postId = post1.id!!, user = testUser)

            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = otherUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = null,
                    viewerId = testUser.id!!,
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns listOf(readLog)

            // when
            val result =
                postListReadService.getPosts(
                    cursor = null,
                    size = 20,
                    currentUserId = testUser.id!!,
                    authorId = otherUser.id!!,
                )

            // then
            val responseMap = result.content.associateBy { it.id }
            assertThat(responseMap[post1.id!!]!!.isRead).isTrue()
            assertThat(responseMap[post2.id!!]!!.isRead).isFalse()
        }
    }
}
