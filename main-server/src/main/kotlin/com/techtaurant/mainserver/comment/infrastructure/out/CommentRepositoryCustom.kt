package com.techtaurant.mainserver.comment.infrastructure.out

import com.techtaurant.mainserver.comment.dto.CommentCursor
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.enums.CommentSortType
import java.util.UUID

/**
 * 댓글 동적 쿼리를 위한 커스텀 Repository
 */
interface CommentRepositoryCustom {
    /**
     * 부모 댓글 목록 조회 (depth=0)
     *
     * @param postId 게시물 ID
     * @param cursor 커서 (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param sortType 정렬 타입
     * @return 부모 댓글 목록
     */
    fun findParentCommentsWithConditions(
        postId: UUID,
        cursor: CommentCursor?,
        size: Int,
        sortType: CommentSortType,
    ): List<Comment>

    /**
     * 대댓글 목록 조회 (depth=1)
     *
     * @param parentId 부모 댓글 ID
     * @param cursor 커서 (null이면 첫 페이지)
     * @param size 페이지 크기
     * @param sortType 정렬 타입
     * @return 대댓글 목록
     */
    fun findRepliesWithConditions(
        parentId: UUID,
        cursor: CommentCursor?,
        size: Int,
        sortType: CommentSortType,
    ): List<Comment>
}
