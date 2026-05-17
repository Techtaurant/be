package com.techtaurant.mainserver.post.entity

/**
 * 게시물 정렬 타입
 *
 * @property field 정렬 기준 필드명
 */
enum class PostSortType(val field: String) {
    /**
     * 최신순 정렬은 updatedAt 기준으로 동작합니다.
     * createdAt을 사용하면 한 번 생성된 게시물이 영구히 낮은 순위에 머물 수 있는 문제가 있어
     * 수정된 게시물도 상단에 노출될 수 있도록 updatedAt을 기준으로 합니다.
     */
    LATEST("updatedAt"),
    VIEW("viewCount"),
    LIKE("likeCount"),
    COMMENT("commentCount"),
    ;

    companion object {
        fun fromString(value: String?): PostSortType = entries.find { it.name.equals(value, ignoreCase = true) } ?: LATEST
    }
}
