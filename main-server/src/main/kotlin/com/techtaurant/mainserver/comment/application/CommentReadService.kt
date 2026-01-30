package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.dto.CommentResponse
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 댓글 읽기 서비스
 * 댓글 조회 로직을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
class CommentReadService(
    private val commentRepository: CommentRepository,
) {

    /**
     * 게시물의 모든 댓글을 조회합니다.
     * 댓글과 대댓글이 생성 시간 오름차순으로 정렬되어 반환됩니다.
     *
     * @param postId 게시물 ID
     * @return 댓글 목록 (생성 시간 오름차순)
     */
    fun getCommentsByPost(postId: UUID): List<CommentResponse> {
        val comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
        return comments.map { CommentResponse.from(it) }
    }
}
