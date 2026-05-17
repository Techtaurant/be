package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.entity.CommentLikeLog
import com.techtaurant.mainserver.comment.infrastructure.out.CommentLikeLogRepository
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepositoryCustom
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.post.entity.Post
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

class CommentViewerStateReadServiceTest {
    private val commentRepository: CommentRepositoryCustom = mockk()
    private val commentLikeLogRepository: CommentLikeLogRepository = mockk()
    private val userBanService: UserBanService = mockk()
    private val commentViewerStateReadService =
        CommentViewerStateReadService(
            commentRepository = commentRepository,
            commentLikeLogRepository = commentLikeLogRepository,
            userBanService = userBanService,
        )

    @Test
    @DisplayName("로그인 사용자별 좋아요와 차단 상태를 댓글 ID 순서대로 반환한다")
    fun getCommentViewerStates_returnsLikeAndBanStatesInRequestOrder() {
        // given
        val viewer = createUser("조회자")
        val author = createUser("일반 작성자")
        val bannedAuthor = createUser("차단 작성자")
        val post = createPost(author)
        val likedComment = createComment(post = post, author = author)
        val bannedComment = createComment(post = post, author = bannedAuthor)

        every {
            commentRepository.findCommentsByIdsIncludingDeleted(listOf(bannedComment.id!!, likedComment.id!!))
        } returns listOf(likedComment, bannedComment)
        every {
            commentLikeLogRepository.findByCommentIdInAndUserId(listOf(bannedComment.id!!, likedComment.id!!), viewer.id!!)
        } returns listOf(CommentLikeLog(comment = likedComment, user = viewer, isLiked = true))
        every { userBanService.getBannedUserIds(viewer.id!!) } returns setOf(bannedAuthor.id!!)

        // when
        val result = commentViewerStateReadService.getCommentViewerStates(viewer.id!!, listOf(bannedComment.id!!, likedComment.id!!))

        // then
        assertThat(result.map { it.commentId }).containsExactly(bannedComment.id, likedComment.id)
        assertThat(result[0].likeStatus).isEqualTo(LikeStatus.NONE)
        assertThat(result[0].isBanned).isTrue()
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

    private fun createComment(
        post: Post,
        author: User,
    ): Comment =
        Comment(
            content = "댓글",
            post = post,
            author = author,
        ).apply { id = UUID.randomUUID() }
}
