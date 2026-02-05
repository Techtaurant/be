package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.PostDailyStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface PostDailyStatsRepository : JpaRepository<PostDailyStats, UUID> {
    /**
     * 특정 게시물의 어제 통계 조회
     *
     * @param postId 게시물 ID
     * @param statDate 통계 날짜
     * @return 해당 날짜의 통계 또는 null
     */
    fun findByPostIdAndStatDate(
        postId: UUID,
        statDate: LocalDate,
    ): PostDailyStats?

    /**
     * 최신 업데이트된 게시물의 통계 조회 (페이징용)
     *
     * @param startDate 조회 시작 날짜
     * @param limit 조회 개수
     * @return 해당 범위의 통계 목록
     */
    @Query(
        """
        SELECT ds FROM PostDailyStats ds
        WHERE ds.statDate >= :startDate
        ORDER BY ds.updatedAt DESC
        LIMIT :limit
        """,
        nativeQuery = false,
    )
    fun findRecentStatsByDate(
        @Param("startDate") startDate: LocalDate,
        @Param("limit") limit: Int,
    ): List<PostDailyStats>
}
