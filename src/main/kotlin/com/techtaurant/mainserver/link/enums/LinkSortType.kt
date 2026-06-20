package com.techtaurant.mainserver.link.enums

/**
 * 공개 링크 목록 정렬 타입
 *
 * - PUBLISHED: 링크 생성일(createdAt) 최신순.
 * - LIKE: 기간 내 일별 좋아요 집계(LinkDailyStats.likeCount) 합산 기준
 * - SAVE: 기간 내 일별 저장 집계(LinkDailyStats.saveCount) 합산 기준
 *
 * LIKE/SAVE는 누적 컬럼이 아니라 기간(period) 윈도우 내 일별 집계 합을 기준으로 합니다.
 */
enum class LinkSortType {
    PUBLISHED,
    LIKE,
    SAVE,
    ;

    companion object {
        fun fromString(value: String?): LinkSortType = entries.find { it.name.equals(value, ignoreCase = true) } ?: PUBLISHED
    }
}
