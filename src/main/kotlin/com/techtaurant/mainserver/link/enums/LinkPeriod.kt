package com.techtaurant.mainserver.link.enums

/**
 * 공개 링크 조회 기간 필터
 *
 * LIKE/SAVE 정렬의 일별 집계 윈도우를 결정합니다. PUBLISHED 정렬에는 적용되지 않습니다.
 *
 * @property days 필터링할 일수 (null이면 전체 기간)
 */
enum class LinkPeriod(val days: Int?) {
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ALL(null),
    ;

    companion object {
        fun fromString(value: String?): LinkPeriod = entries.find { it.name.equals(value, ignoreCase = true) } ?: ALL
    }
}
