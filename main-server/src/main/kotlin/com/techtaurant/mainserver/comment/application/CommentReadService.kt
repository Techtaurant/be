package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.comment.dto.CommentCursor
import com.techtaurant.mainserver.comment.dto.CommentListResponse
import com.techtaurant.mainserver.comment.enums.CommentSortType
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.dto.CursorPageResponse
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
     * 부모 댓글 목록을 커서 기반 페이지네이션으로 조회합니다.
     *
     * @param postId 게시물 ID
     * @param cursor 이전 응답의 nextCursor (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param sortType 정렬 기준 (LATEST, LIKE, REPLY)
     * @return 커서 기반 페이지 응답
     */
    fun getParentComments(
        postId: UUID,
        cursor: String?,
        size: Int,
        sortType: CommentSortType = CommentSortType.LATEST,
    ): CursorPageResponse<CommentListResponse> {
        val commentCursor = cursor?.let { CommentCursor.decode(it) }

        if (cursor != null && commentCursor == null) {
            return CursorPageResponse(
                content = emptyList(),
                nextCursor = null,
                hasNext = false,
                size = 0,
            )
        }

        val comments =
            commentRepository.findParentCommentsWithConditions(
                postId = postId,
                cursor = commentCursor,
                size = size + 1,
                sortType = sortType,
            )

        val hasNext = comments.size > size
        val content = comments.take(size)

        val nextCursor =
            if (hasNext && content.isNotEmpty()) {
                CommentCursor.from(content.last(), sortType).encode()
            } else {
                null
            }

        return CursorPageResponse(
            content = content.map { CommentListResponse.from(it) },
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }

    /**
     * 대댓글 목록을 커서 기반 페이지네이션으로 조회합니다.
     *
     * @param parentId 부모 댓글 ID
     * @param cursor 이전 응답의 nextCursor (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param sortType 정렬 기준 (LATEST, LIKE, REPLY)
     * @return 커서 기반 페이지 응답
     */
    fun getReplies(
        parentId: UUID,
        cursor: String?,
        size: Int,
        sortType: CommentSortType = CommentSortType.LATEST,
    ): CursorPageResponse<CommentListResponse> {
        val commentCursor = cursor?.let { CommentCursor.decode(it) }

        if (cursor != null && commentCursor == null) {
            return CursorPageResponse(
                content = emptyList(),
                nextCursor = null,
                hasNext = false,
                size = 0,
            )
        }

        val comments =
            commentRepository.findRepliesWithConditions(
                parentId = parentId,
                cursor = commentCursor,
                size = size + 1,
                sortType = sortType,
            )

        val hasNext = comments.size > size
        val content = comments.take(size)

        val nextCursor =
            if (hasNext && content.isNotEmpty()) {
                CommentCursor.from(content.last(), sortType).encode()
            } else {
                null
            }

        return CursorPageResponse(
            content = content.map { CommentListResponse.from(it) },
            nextCursor = nextCursor,
            hasNext = hasNext,
            size = content.size,
        )
    }
}
