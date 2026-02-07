package com.techtaurant.mainserver.comment.enums

/**
 * 댓글 정렬 타입
 *
 * @property field 정렬 기준 필드명
 */
enum class CommentSortType(val field: String) {
    LATEST("createdAt"),
    LIKE("likeCount"),
    REPLY("replyCount"),
    ;

    companion object {
        fun fromString(value: String?): CommentSortType = entries.find { it.name.equals(value, ignoreCase = true) } ?: LATEST
    }
}
