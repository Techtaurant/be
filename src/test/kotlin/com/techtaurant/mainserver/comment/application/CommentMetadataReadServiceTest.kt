package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepositoryCustom
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CommentMetadataReadServiceTest {
    private val commentRepository: CommentRepositoryCustom = mockk()
    private val commentMetadataReadService = CommentMetadataReadService(commentRepository)

    @Test
    @DisplayName("metadata는 좋아요수, 대댓글수, 삭제 여부를 요청 댓글 ID 순서대로 반환한다")
    fun getCommentMetadata_returnsLikeCountAndDeletedStateInRequestOrder() {
        // given
        val author = createUser("작성자")
        val post = createPost(author)
        val activeComment = createComment(post = post, author = author, likeCount = 7, replyCount = 2)
        val deletedComment = createComment(post = post, author = author, likeCount = 3, replyCount = 1).apply { deletedAt = Instant.now() }

        every {
            commentRepository.findCommentsByIdsIncludingDeleted(listOf(deletedComment.id!!, activeComment.id!!))
        } returns listOf(activeComment, deletedComment)

        // when
        val result = commentMetadataReadService.getCommentMetadata(listOf(deletedComment.id!!, activeComment.id!!))

        // then
        assertThat(result.map { it.commentId }).containsExactly(deletedComment.id, activeComment.id)
        assertThat(result[0].likeCount).isEqualTo(3)
        assertThat(result[0].replyCount).isEqualTo(1)
        assertThat(result[0].isDeleted).isTrue()
        assertThat(result[1].likeCount).isEqualTo(7)
        assertThat(result[1].replyCount).isEqualTo(2)
        assertThat(result[1].isDeleted).isFalse()
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
        likeCount: Long,
        replyCount: Long,
    ): Comment =
        Comment(
            content = "댓글",
            post = post,
            author = author,
            likeCount = likeCount,
            replyCount = replyCount,
        ).apply { id = UUID.randomUUID() }
}
