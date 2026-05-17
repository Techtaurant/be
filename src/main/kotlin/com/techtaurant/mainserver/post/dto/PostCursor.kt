package com.techtaurant.mainserver.post.dto

import com.techtaurant.mainserver.post.entity.PostSortType
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * 게시물 목록 커서
 *
 * 정렬 기준에 따라 다른 커서 값을 사용
 * - LATEST: updatedAt(최신 수정 시간), id
 * - VIEW/LIKE/COMMENT: sortValue(해당 count), createdAt, id
 *
 * @property sortValue 정렬 기준 값 (조회수, 좋아요수, 댓글수)
 * @property createdAt LATEST 정렬 시 updatedAt, 그 외 정렬 시 createdAt을 담는 보조 정렬 시간 필드
 * @property id 게시물 ID
 * @property sortType 정렬 타입
 */
data class PostCursor(
    val sortValue: Long,
    val createdAt: Date,
    val id: UUID,
    val sortType: PostSortType,
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
         * Base64 인코딩된 커서 문자열을 PostCursor로 디코딩
         *
         * @param cursor Base64 인코딩된 커서 문자열
         * @return 디코딩된 PostCursor, 실패 시 null
         */
        fun decode(cursor: String): PostCursor? {
            return try {
                val decoded = String(Base64.getUrlDecoder().decode(cursor))
                val parts = decoded.split(":")
                if (parts.size != 4) return null

                val sortType = PostSortType.fromString(parts[0])
                val sortValue = parts[1].toLongOrNull() ?: return null
                val timestamp = parts[2].toLongOrNull() ?: return null
                val uuid = UUID.fromString(parts[3])

                PostCursor(sortValue, Date(timestamp), uuid, sortType)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Post 엔티티에서 커서 생성
         *
         * @param post 게시물 엔티티
         * @param sortType 정렬 타입
         */
        fun from(
            post: com.techtaurant.mainserver.post.entity.Post,
            sortType: PostSortType,
        ): PostCursor {
            val sortValue =
                when (sortType) {
                    PostSortType.LATEST -> 0L
                    PostSortType.VIEW -> post.viewCount
                    PostSortType.LIKE -> post.likeCount
                    PostSortType.COMMENT -> post.commentCount
                }
            // LATEST 정렬은 updatedAt 기준이므로 커서에 updatedAt을 저장합니다.
            val cursorDate = if (sortType == PostSortType.LATEST) post.updatedAt else post.createdAt
            return PostCursor(sortValue, cursorDate, post.id!!, sortType)
        }
    }
}
