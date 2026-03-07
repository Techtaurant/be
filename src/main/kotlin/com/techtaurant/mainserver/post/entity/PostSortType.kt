package com.techtaurant.mainserver.post.entity

/**
 * 게시물 정렬 타입
 *
 * @property field 정렬 기준 필드명
 */
enum class PostSortType(val field: String) {
    LATEST("createdAt"),
    VIEW("viewCount"),
    LIKE("likeCount"),
    COMMENT("commentCount"),
    ;

    companion object {
        fun fromString(value: String?): PostSortType = entries.find { it.name.equals(value, ignoreCase = true) } ?: LATEST
    }
}
