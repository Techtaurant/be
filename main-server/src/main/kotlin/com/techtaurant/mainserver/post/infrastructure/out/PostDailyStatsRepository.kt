package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.PostDailyStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface PostDailyStatsRepository : JpaRepository<PostDailyStats, UUID> {
    /**
     * 특정 게시물의 일별 조회수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     * @param statDate 통계 날짜
     * @return 업데이트된 행 수 (0이면 해당 날짜 레코드 미존재)
     */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        UPDATE post_daily_stats
        SET view_count = view_count + 1, updated_at = NOW()
        WHERE post_id = :postId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun incrementViewCount(
        @Param("postId") postId: UUID,
        @Param("statDate") statDate: LocalDate,
    ): Int

    /**
     * 특정 게시물의 일별 좋아요수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     * @param statDate 통계 날짜
     * @return 업데이트된 행 수 (0이면 해당 날짜 레코드 미존재)
     */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        UPDATE post_daily_stats
        SET like_count = like_count + 1, updated_at = NOW()
        WHERE post_id = :postId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun incrementLikeCount(
        @Param("postId") postId: UUID,
        @Param("statDate") statDate: LocalDate,
    ): Int

    /**
     * 특정 게시물의 일별 좋아요수를 원자적으로 1 감소시킵니다.
     * 음수 방지를 위해 0 미만으로 내려가지 않습니다.
     *
     * @param postId 게시물 ID
     * @param statDate 통계 날짜
     * @return 업데이트된 행 수 (0이면 해당 날짜 레코드 미존재)
     */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        UPDATE post_daily_stats
        SET like_count = GREATEST(like_count - 1, 0), updated_at = NOW()
        WHERE post_id = :postId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun decrementLikeCount(
        @Param("postId") postId: UUID,
        @Param("statDate") statDate: LocalDate,
    ): Int

    /**
     * 특정 게시물의 일별 댓글수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     * @param statDate 통계 날짜
     * @return 업데이트된 행 수 (0이면 해당 날짜 레코드 미존재)
     */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        UPDATE post_daily_stats
        SET comment_count = comment_count + 1, updated_at = NOW()
        WHERE post_id = :postId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun incrementCommentCount(
        @Param("postId") postId: UUID,
        @Param("statDate") statDate: LocalDate,
    ): Int
}
