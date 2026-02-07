package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.dto.CommentResponse
import com.techtaurant.mainserver.comment.dto.CreateCommentRequest
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.application.PostDailyStatsService
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.application.PostDailyStatsService
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 댓글 쓰기 서비스
 * 댓글 생성 및 검증 로직을 담당합니다.
 */
@Service
class CommentWriteService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val postDailyStatsService: PostDailyStatsService,
) {
    /**
     * 댓글을 생성합니다.
     * 게시물 존재 확인, 부모 댓글 검증을 수행합니다.
     *
     * @param userId 댓글 작성자 ID
     * @param request 댓글 생성 요청
     * @return 생성된 댓글 응답
     * @throws ApiException 게시물을 찾을 수 없음, 부모 댓글을 찾을 수 없음,
     *                     부모 댓글이 다른 게시물의 댓글, 대대댓글 시도
     */
    @Transactional
    fun createComment(
        userId: UUID,
        request: CreateCommentRequest,
    ): CommentResponse {
        val (post, author) = validatePostAndAuthor(request.postId, userId)
        val parent = resolveParent(request.parentId, request.postId)

        val depth = if (parent == null) 0 else 1

        val comment =
            Comment(
                content = request.content,
                post = post,
                author = author,
                parent = parent,
                depth = depth,
            )

        val savedComment = commentRepository.save(comment)

        postRepository.incrementCommentCount(request.postId)
        postDailyStatsService.incrementCommentCount(request.postId)

        return CommentResponse.from(savedComment)
    }

    /**
     * 게시물과 작성자를 검증합니다.
     *
     * @param postId 게시물 ID
     * @param userId 작성자 ID
     * @return 검증된 Post와 User의 Pair
     * @throws ApiException 게시물 또는 사용자를 찾을 수 없음
     */
    private fun validatePostAndAuthor(
        postId: UUID,
        userId: UUID,
    ): Pair<Post, User> {
        val post =
            postRepository.findById(postId).orElseThrow {
                ApiException(PostStatus.POST_NOT_FOUND)
            }
        val user =
            userRepository.findById(userId).orElseThrow {
                ApiException(UserStatus.ID_NOT_FOUND)
            }
        return Pair(post, user)
    }

    /**
     * 부모 댓글을 검증합니다.
     * parentId가 null인 경우 null을 반환하고,
     * parentId가 있으면 부모 댓글이 존재하는지, 같은 게시물의 댓글인지, depth가 0인지 확인합니다.
     *
     * @param parentId 부모 댓글 ID (nullable)
     * @param postId 현재 게시물 ID
     * @return 부모 댓글 또는 null
     * @throws ApiException 부모 댓글을 찾을 수 없음, 부모 댓글이 다른 게시물,
     *                     대댓글의 답글 시도
     */
    private fun resolveParent(
        parentId: UUID?,
        postId: UUID,
    ): Comment? {
        if (parentId == null) {
            return null
        }

        val parent =
            commentRepository.findById(parentId).orElseThrow {
                ApiException(CommentStatus.COMMENT_NOT_FOUND)
            }

        if (parent.post.id != postId) {
            throw ApiException(CommentStatus.COMMENT_PARENT_MISMATCH)
        }

        if (parent.depth != 0) {
            throw ApiException(CommentStatus.COMMENT_MAX_DEPTH_EXCEEDED)
        }

        return parent
    }
}
