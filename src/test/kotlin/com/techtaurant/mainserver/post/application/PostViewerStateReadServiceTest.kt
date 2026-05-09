package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostLikeLog
import com.techtaurant.mainserver.post.entity.PostReadLog
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.application.UserBanService
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class PostViewerStateReadServiceTest {
    private val postRepository: PostRepository = mockk()
    private val postReadLogRepository: PostReadLogRepository = mockk()
    private val postLikeLogRepository: PostLikeLogRepository = mockk()
    private val userBanService: UserBanService = mockk()

    private val postViewerStateReadService =
        PostViewerStateReadService(
            postRepository = postRepository,
            postReadLogRepository = postReadLogRepository,
            postLikeLogRepository = postLikeLogRepository,
            userBanService = userBanService,
        )

    @Test
    @DisplayName("로그인 사용자별 읽음, 좋아요, 차단 상태를 게시물 ID 순서대로 반환한다")
    fun getPostViewerStates_returnsReadLikeAndBanStatesInRequestOrder() {
        // given
        val viewer = createUser("조회자")
        val visibleAuthor = createUser("일반 작성자")
        val bannedAuthor = createUser("차단 작성자")
        val readPost = createPost(visibleAuthor)
        val bannedPost = createPost(bannedAuthor)
        val likeLog = PostLikeLog(post = readPost, user = viewer, isLiked = true)

        every { postRepository.findPublishedPostsByIdIn(listOf(bannedPost.id!!, readPost.id!!)) } returns listOf(readPost, bannedPost)
        every { postReadLogRepository.findByUserIdAndPostIdIn(viewer.id!!, listOf(readPost.id!!, bannedPost.id!!)) } returns
            listOf(PostReadLog(postId = readPost.id!!, user = viewer))
        every { postLikeLogRepository.findByUserIdAndPostIdIn(viewer.id!!, listOf(readPost.id!!, bannedPost.id!!)) } returns listOf(likeLog)
        every { userBanService.getBannedUserIds(viewer.id!!) } returns setOf(bannedAuthor.id!!)

        // when
        val result = postViewerStateReadService.getPostViewerStates(viewer.id!!, listOf(bannedPost.id!!, readPost.id!!))

        // then
        assertThat(result.map { it.postId }).containsExactly(bannedPost.id, readPost.id)
        assertThat(result[0].isRead).isFalse()
        assertThat(result[0].likeStatus).isEqualTo(LikeStatus.NONE)
        assertThat(result[0].isBanned).isTrue()
        assertThat(result[1].isRead).isTrue()
        assertThat(result[1].likeStatus).isEqualTo(LikeStatus.LIKE)
        assertThat(result[1].isBanned).isFalse()
    }

    private fun createUser(name: String): User =
        User(
            name = name,
            email = "${UUID.randomUUID()}@example.com",
            provider = OAuthProvider.GOOGLE,
            identifier = UUID.randomUUID().toString(),
            role = UserRole.USER,
            profileImageUrl = "https://example.com/profile.jpg",
        ).apply { id = UUID.randomUUID() }

    private fun createPost(author: User): Post =
        Post(
            title = "게시물",
            content = "본문",
            author = author,
        ).apply { id = UUID.randomUUID() }
}
