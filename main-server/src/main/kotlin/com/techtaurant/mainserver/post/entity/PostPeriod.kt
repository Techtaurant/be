package com.techtaurant.mainserver.post.entity

/**
 * 게시물 조회 기간 필터
 *
 * @property days 필터링할 일수 (null이면 전체 기간)
 */
enum class PostPeriod(val days: Int?) {
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ALL(null),
    ;

    companion object {
        fun fromString(value: String?): PostPeriod = entries.find { it.name.equals(value, ignoreCase = true) } ?: ALL
    }
}
