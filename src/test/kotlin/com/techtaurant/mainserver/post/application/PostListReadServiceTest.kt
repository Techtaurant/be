package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.attachment.application.AttachmentService
import com.techtaurant.mainserver.attachment.entity.Attachment
import com.techtaurant.mainserver.attachment.enums.AttachmentReferenceType
import com.techtaurant.mainserver.attachment.enums.AttachmentStatus
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostPeriod
import com.techtaurant.mainserver.post.entity.PostReadLog
import com.techtaurant.mainserver.post.entity.PostSortType
import com.techtaurant.mainserver.post.enums.PostStatusEnum
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.application.UserProfileImageResolver
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

class PostListReadServiceTest {
    private val postRepository: PostRepository = mockk()
    private val postReadLogRepository: PostReadLogRepository = mockk()
    private val attachmentService: AttachmentService = mockk()
    private val userProfileImageResolver = UserProfileImageResolver(attachmentService)
    private val defaultThumbnailUrl = "/static/images/post-thumbnail.png"
    private val baseUrl = "http://localhost:8080"

    private val postListReadService =
        createPostListReadService()

    private fun createPostListReadService(postListQueryStrategies: List<PostListQueryStrategy> = createPostListQueryStrategies()) =
        PostListReadService(
            postRepository = postRepository,
            postReadLogRepository = postReadLogRepository,
            attachmentService = attachmentService,
            userProfileImageResolver = userProfileImageResolver,
            postListQueryStrategies = postListQueryStrategies,
            defaultThumbnailUrl = defaultThumbnailUrl,
            baseUrl = baseUrl,
        )

    private fun createPostListQueryStrategies(): List<PostListQueryStrategy> =
        listOf(
            AllVisiblePostsQueryStrategy(postRepository),
            OwnVisiblePostsQueryStrategy(postRepository),
            AuthorPublicPostsQueryStrategy(postRepository),
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
        thumbnailImage: UUID? = null,
        category: Category? = null,
    ): Post =
        Post(
            title = "테스트 게시물",
            content = "테스트 내용",
            author = author,
            thumbnailImage = thumbnailImage,
            category = category,
            status = status,
        ).apply { id = UUID.randomUUID() }

    private fun createAttachment(
        postId: UUID,
        objectKey: String,
        createdAt: Date,
    ): Attachment =
        Attachment(
            referenceId = postId,
            referenceType = AttachmentReferenceType.POST,
            objectKey = objectKey,
            status = AttachmentStatus.CONFIRMED,
            originalFileName = "thumbnail.jpg",
            contentType = "image/jpeg",
            fileSize = 1024,
        ).apply {
            id = UUID.randomUUID()
            this.createdAt = createdAt
        }

    private fun createUserProfileAttachment(
        userId: UUID,
        attachmentId: UUID,
    ): Attachment =
        Attachment(
            referenceId = userId,
            referenceType = AttachmentReferenceType.USER,
            objectKey = "users/$userId/$attachmentId/profile.png",
            status = AttachmentStatus.CONFIRMED,
            originalFileName = "profile.png",
            contentType = "image/png",
            fileSize = 1024L,
        ).apply { id = attachmentId }

    private fun createCategory(
        user: User,
        name: String = "백엔드",
        path: String = "backend",
        depth: Int = 1,
        parent: Category? = null,
    ): Category =
        Category(
            user = user,
            name = name,
            path = path,
            depth = depth,
            parent = parent,
        ).apply { id = UUID.randomUUID() }

    @Test
    @DisplayName("게시물 목록 조회 전략이 누락되면 서비스 생성에 실패한다")
    fun constructor_missingStrategy_throwsException() {
        assertThatThrownBy {
            createPostListReadService(
                postListQueryStrategies =
                    listOf(
                        AllVisiblePostsQueryStrategy(postRepository),
                        OwnVisiblePostsQueryStrategy(postRepository),
                    ),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("누락")
    }

    @Test
    @DisplayName("게시물 목록 조회 전략이 중복되면 서비스 생성에 실패한다")
    fun constructor_duplicateStrategy_throwsException() {
        assertThatThrownBy {
            createPostListReadService(
                postListQueryStrategies =
                    listOf(
                        AllVisiblePostsQueryStrategy(postRepository),
                        AllVisiblePostsQueryStrategy(postRepository),
                        OwnVisiblePostsQueryStrategy(postRepository),
                        AuthorPublicPostsQueryStrategy(postRepository),
                    ),
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("중복")
    }

    @Nested
    @DisplayName("getPosts")
    inner class GetPosts {
        @BeforeEach
        fun setUp() {
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(any(), AttachmentReferenceType.POST)
            } returns emptyMap()
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(any(), AttachmentReferenceType.USER)
            } returns emptyMap()
            every { attachmentService.generatePresignedDownloadUrlMapByAttachments(emptyList()) } returns emptyMap()
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
                    tagIds = null,
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
                    tagIds = null,
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
                    tagIds = null,
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
                    tagIds = null,
                    viewerId = null,
                )
            }
        }

        @Test
        @DisplayName("태그 ID 필터 전달 시 중복 제거된 UUID 목록으로 Repository를 호출한다")
        fun getPosts_withTagIds_passesDistinctTagIds() {
            // given
            val posts = listOf(createPost(otherUser))
            val firstTagId = UUID.randomUUID()
            val secondTagId = UUID.randomUUID()
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                    tagIds = listOf(firstTagId, secondTagId),
                    viewerId = null,
                )
            } returns posts

            // when
            postListReadService.getPosts(
                cursor = null,
                size = 20,
                currentUserId = null,
                tagIds = listOf(firstTagId, secondTagId, firstTagId),
            )

            // then
            verify {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                    tagIds = listOf(firstTagId, secondTagId),
                    viewerId = null,
                )
            }
        }

        @Test
        @DisplayName("첨부파일 썸네일은 presigned URL로 반환한다")
        fun getPosts_withThumbnailAttachment_returnsPresignedThumbnailUrl() {
            // given
            val post = createPost(otherUser)
            val firstAttachment =
                createAttachment(
                    postId = post.id!!,
                    objectKey = "posts/${post.id}/uuid-1/first.jpg",
                    createdAt = Date(1_000L),
                )
            val laterAttachment =
                createAttachment(
                    postId = post.id!!,
                    objectKey = "posts/${post.id}/uuid-2/later.jpg",
                    createdAt = Date(2_000L),
                )
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                    tagIds = null,
                    viewerId = null,
                )
            } returns listOf(post)
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(listOf(post.id!!), AttachmentReferenceType.POST)
            } returns mapOf(post.id!! to listOf(laterAttachment, firstAttachment))
            every {
                attachmentService.generatePresignedDownloadUrlMapByAttachments(listOf(firstAttachment))
            } returns mapOf(firstAttachment.id!! to "https://cdn.example.com/first.jpg")

            // when
            val result = postListReadService.getPosts(cursor = null, size = 20, currentUserId = null)

            // then
            assertThat(result.content.single().thumbnailUrl).isEqualTo("https://cdn.example.com/first.jpg")
        }

        @Test
        @DisplayName("게시물에 thumbnailImage가 있으면 attachment fallback 없이 해당 값을 사용한다")
        fun getPosts_withThumbnailImage_usesThumbnailImageFirst() {
            // given
            val thumbnailAttachmentId = UUID.randomUUID()
            val post = createPost(otherUser, thumbnailImage = thumbnailAttachmentId)
            val thumbnailAttachment =
                createAttachment(
                    post.id!!,
                    "posts/thumbnail-object-key.jpg",
                    Date(1_000L),
                ).apply { id = thumbnailAttachmentId }
            val otherAttachment = createAttachment(post.id!!, "posts/other-object-key.jpg", Date(2_000L))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    visibleToUserId = null,
                    tagIds = null,
                    viewerId = null,
                )
            } returns listOf(post)
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(listOf(post.id!!), AttachmentReferenceType.POST)
            } returns mapOf(post.id!! to listOf(otherAttachment, thumbnailAttachment))
            every {
                attachmentService.generatePresignedDownloadUrlMapByAttachments(listOf(thumbnailAttachment))
            } returns mapOf(thumbnailAttachmentId to "https://cdn.example.com/thumbnail.jpg")

            // when
            val result = postListReadService.getPosts(cursor = null, size = 20, currentUserId = null)

            // then
            assertThat(result.content.single().thumbnailUrl).isEqualTo("https://cdn.example.com/thumbnail.jpg")
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
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(any(), AttachmentReferenceType.USER)
            } returns emptyMap()
            every { attachmentService.generatePresignedDownloadUrlMapByAttachments(emptyList()) } returns emptyMap()
        }

        @Test
        @DisplayName("본인 조회 시 DRAFT를 제외한 공개 가능 상태만 Repository에 전달한다")
        fun getPosts_ownPosts_excludesDraftStatus() {
            // given
            val posts = listOf(createPost(testUser))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = testUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED, PostStatusEnum.PRIVATE),
                    categoryId = null,
                    tagIds = null,
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
                    statuses = listOf(PostStatusEnum.PUBLISHED, PostStatusEnum.PRIVATE),
                    categoryId = null,
                    tagIds = null,
                    viewerId = testUser.id!!,
                )
            }
        }

        @Test
        @DisplayName("본인 조회 시 Repository 상태 필터에 DRAFT가 포함되지 않는다")
        fun getPosts_ownPosts_doesNotPassDraftStatus() {
            // given
            val posts = listOf(createPost(testUser, status = PostStatusEnum.PRIVATE))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = testUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED, PostStatusEnum.PRIVATE),
                    categoryId = null,
                    tagIds = null,
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
            verify(exactly = 0) {
                postRepository.findPostsWithConditions(
                    cursor = any(),
                    size = any(),
                    period = any(),
                    sortType = any(),
                    authorId = testUser.id!!,
                    statuses = match { PostStatusEnum.DRAFT in it },
                    categoryId = any(),
                    tagIds = any(),
                    viewerId = any(),
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
                    tagIds = null,
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
                    tagIds = null,
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
                    tagIds = null,
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
                    tagIds = null,
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
                    statuses = listOf(PostStatusEnum.PUBLISHED, PostStatusEnum.PRIVATE),
                    categoryId = null,
                    tagIds = null,
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
                    statuses = listOf(PostStatusEnum.PUBLISHED, PostStatusEnum.PRIVATE),
                    categoryId = null,
                    tagIds = null,
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
                    tagIds = null,
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

        @Test
        @DisplayName("게시물 카테고리가 있으면 응답에 함께 포함된다")
        fun getPosts_includesCategoryInResponse() {
            // given
            val category = createCategory(otherUser, name = "Kotlin", path = "backend/kotlin", depth = 2)
            val posts = listOf(createPost(otherUser, category = category))
            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = otherUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = null,
                    tagIds = null,
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
            val response = result.content.single()
            assertThat(response.category).isNotNull
            assertThat(response.category!!.id).isEqualTo(category.id)
            assertThat(response.category!!.name).isEqualTo(category.name)
            assertThat(response.category!!.path).isEqualTo(category.path)
            assertThat(response.category!!.depth).isEqualTo(category.depth)
        }

        @Test
        @DisplayName("서비스 프로필 이미지 attachment가 있으면 작성자 프로필 이미지에 presigned URL을 사용한다")
        fun getPosts_serviceProfileImageAttachment_usesPresignedAuthorProfileImageUrl() {
            val attachmentId = UUID.randomUUID()
            otherUser.serviceProfileImageAttachmentId = attachmentId
            val posts = listOf(createPost(otherUser))
            val profileAttachment = createUserProfileAttachment(otherUser.id!!, attachmentId)

            every {
                postRepository.findPostsWithConditions(
                    cursor = null,
                    size = 21,
                    period = PostPeriod.ALL,
                    sortType = PostSortType.LATEST,
                    authorId = otherUser.id!!,
                    statuses = listOf(PostStatusEnum.PUBLISHED),
                    categoryId = null,
                    tagIds = null,
                    viewerId = null,
                )
            } returns posts
            every {
                attachmentService.getConfirmedAttachmentsByReferenceIds(listOf(otherUser.id!!), AttachmentReferenceType.USER)
            } returns mapOf(otherUser.id!! to listOf(profileAttachment))
            every {
                attachmentService.generatePresignedDownloadUrlMapByAttachments(listOf(profileAttachment))
            } returns mapOf(attachmentId to "https://cdn.example.com/authors/other-user.png")

            val result =
                postListReadService.getPosts(
                    cursor = null,
                    size = 20,
                    currentUserId = null,
                    authorId = otherUser.id!!,
                )

            assertThat(result.content.single().authorProfileImageUrl)
                .isEqualTo("https://cdn.example.com/authors/other-user.png")
        }
    }
}
