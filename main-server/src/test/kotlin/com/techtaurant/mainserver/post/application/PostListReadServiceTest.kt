package com.techtaurant.mainserver.post.application

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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class PostListReadServiceTest {
    private val postRepository: PostRepository = mockk()
    private val postReadLogRepository: PostReadLogRepository = mockk()
    private val defaultThumbnailUrl = "/static/images/post-thumbnail.png"

    private val postListReadService =
        PostListReadService(
            postRepository = postRepository,
            postReadLogRepository = postReadLogRepository,
            defaultThumbnailUrl = defaultThumbnailUrl,
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

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun setCurrentUser(user: User?) {
        if (user != null) {
            val authentication = mockk<Authentication>()
            every { authentication.principal } returns user.id!!
            val securityContext = mockk<SecurityContext>()
            every { securityContext.authentication } returns authentication
            SecurityContextHolder.setContext(securityContext)
        } else {
            SecurityContextHolder.clearContext()
        }
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
        @Test
        @DisplayName("로그인 사용자 조회 시 visibleToUserId에 현재 사용자 ID를 전달한다")
        fun getPosts_loggedInUser_passesVisibleToUserId() {
            // given
            setCurrentUser(testUser)
            val posts = listOf(createPost(testUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = testUser.id!!,
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            postListReadService.getPosts(cursor = null, size = 20)

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = testUser.id!!,
                )
            }
        }

        @Test
        @DisplayName("비로그인 사용자 조회 시 visibleToUserId에 null을 전달한다")
        fun getPosts_anonymousUser_passesNullVisibleToUserId() {
            // given
            setCurrentUser(null)
            val posts = listOf(createPost(otherUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                )
            } returns posts

            // when
            postListReadService.getPosts(cursor = null, size = 20)

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                )
            }
        }
    }

    @Nested
    @DisplayName("getPostsByUserId")
    inner class GetPostsByUserId {
        @Test
        @DisplayName("본인 조회 시 모든 상태(DRAFT, PUBLISHED, PRIVATE)로 Repository를 호출한다")
        fun getPostsByUserId_ownPosts_queriesAllStatuses() {
            // given
            setCurrentUser(testUser)
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
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            postListReadService.getPostsByUserId(
                userId = testUser.id!!,
                cursor = null,
                size = 20,
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
                )
            }
        }

        @Test
        @DisplayName("타인 조회 시 PUBLISHED만으로 Repository를 호출한다")
        fun getPostsByUserId_otherUserPosts_queriesPublishedOnly() {
            // given
            setCurrentUser(testUser)
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
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            postListReadService.getPostsByUserId(
                userId = otherUser.id!!,
                cursor = null,
                size = 20,
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
                )
            }
        }

        @Test
        @DisplayName("비로그인 사용자 조회 시 PUBLISHED만으로 Repository를 호출하고 isRead는 항상 false이다")
        fun getPostsByUserId_anonymousUser_queriesPublishedOnly() {
            // given
            setCurrentUser(null)
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
                )
            } returns posts

            // when
            val result =
                postListReadService.getPostsByUserId(
                    userId = otherUser.id!!,
                    cursor = null,
                    size = 20,
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
                )
            }
            assertThat(result.content).allSatisfy { assertThat(it.isRead).isFalse() }
        }

        @Test
        @DisplayName("잘못된 커서 전달 시 빈 응답을 반환한다")
        fun getPostsByUserId_invalidCursor_returnsEmptyResponse() {
            // given
            setCurrentUser(testUser)

            // when
            val result =
                postListReadService.getPostsByUserId(
                    userId = testUser.id!!,
                    cursor = "invalid-cursor-string",
                    size = 20,
                )

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.nextCursor).isNull()
            assertThat(result.hasNext).isFalse()
            assertThat(result.size).isEqualTo(0)
        }

        @Test
        @DisplayName("다음 페이지가 있으면 hasNext가 true이고 nextCursor가 반환된다")
        fun getPostsByUserId_hasMorePosts_returnsHasNextTrue() {
            // given
            setCurrentUser(testUser)
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
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            val result =
                postListReadService.getPostsByUserId(
                    userId = testUser.id!!,
                    cursor = null,
                    size = 2,
                )

            // then
            assertThat(result.hasNext).isTrue()
            assertThat(result.nextCursor).isNotNull()
            assertThat(result.content).hasSize(2)
        }

        @Test
        @DisplayName("다음 페이지가 없으면 hasNext가 false이고 nextCursor가 null이다")
        fun getPostsByUserId_noMorePosts_returnsHasNextFalse() {
            // given
            setCurrentUser(testUser)
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
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns emptyList()

            // when
            val result =
                postListReadService.getPostsByUserId(
                    userId = testUser.id!!,
                    cursor = null,
                    size = 20,
                )

            // then
            assertThat(result.hasNext).isFalse()
            assertThat(result.nextCursor).isNull()
            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("로그인 사용자가 조회 시 읽음 기록이 반영된다")
        fun getPostsByUserId_loggedInUser_appliesReadStatus() {
            // given
            setCurrentUser(testUser)
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
                )
            } returns posts
            every {
                postReadLogRepository.findByUserIdAndPostIdIn(testUser.id!!, any())
            } returns listOf(readLog)

            // when
            val result =
                postListReadService.getPostsByUserId(
                    userId = otherUser.id!!,
                    cursor = null,
                    size = 20,
                )

            // then
            val responseMap = result.content.associateBy { it.id }
            assertThat(responseMap[post1.id!!]!!.isRead).isTrue()
            assertThat(responseMap[post2.id!!]!!.isRead).isFalse()
        }
    }
}
