package com.techtaurant.mainserver.comment.dto

import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.enums.CommentSortType
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * 댓글 목록 커서
 *
 * 정렬 기준에 따라 다른 커서 값을 사용
 * - LATEST: createdAt, id
 * - LIKE/REPLY: sortValue(해당 count), createdAt, id
 *
 * @property sortValue 정렬 기준 값 (좋아요수, 대댓글수)
 * @property createdAt 댓글 생성 시간
 * @property id 댓글 ID
 * @property sortType 정렬 타입
 */
data class CommentCursor(
    val sortValue: Long,
    val createdAt: Date,
    val id: UUID,
    val sortType: CommentSortType,
) {
    /**
     * 커서를 Base64 인코딩된 문자열로 변환
     */
    fun encode(): String {
        val raw = "${sortType.name}:$sortValue:${createdAt.time}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    companion object {
        /**
         * Base64 인코딩된 커서 문자열을 CommentCursor로 디코딩
         *
         * @param cursor Base64 인코딩된 커서 문자열
         * @return 디코딩된 CommentCursor, 실패 시 null
         */
        fun decode(cursor: String): CommentCursor? {
            val decoded = String(Base64.getUrlDecoder().decode(cursor))
            val parts = decoded.split(":")
            if (parts.size != 4) return null

            val sortType = CommentSortType.fromString(parts[0])
            val sortValue = parts[1].toLongOrNull() ?: return null
            val timestamp = parts[2].toLongOrNull() ?: return null
            val uuid = UUID.fromString(parts[3])

            return CommentCursor(sortValue, Date(timestamp), uuid, sortType)
        }

        /**
         * Comment 엔티티에서 커서 생성
         *
         * @param comment 댓글 엔티티
         * @param sortType 정렬 타입
         */
        fun from(
            comment: Comment,
            sortType: CommentSortType,
        ): CommentCursor {
            val sortValue =
                when (sortType) {
                    CommentSortType.LATEST -> 0L
                    CommentSortType.LIKE -> comment.likeCount
                    CommentSortType.REPLY -> comment.replyCount
                }
            return CommentCursor(sortValue, comment.createdAt, comment.id!!, sortType)
        }
    }
}
