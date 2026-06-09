package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.PostDailyStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

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
        SET view_count = view_count + 1, updated_at_utc = NOW()
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
        SET like_count = like_count + 1, updated_at_utc = NOW()
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
     * 날짜가 다른 경우(어제 좋아요 → 오늘 싫어요) 음수 값을 가질 수 있습니다.
     *
     * @param postId 게시물 ID
     * @param statDate 통계 날짜
     * @return 업데이트된 행 수 (0이면 해당 날짜 레코드 미존재)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE post_daily_stats
        SET like_count = like_count - 1, updated_at_utc = NOW()
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
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE post_daily_stats
        SET comment_count = comment_count + 1, updated_at_utc = NOW()
        WHERE post_id = :postId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun incrementCommentCount(
        @Param("postId") postId: UUID,
        @Param("statDate") statDate: LocalDate,
    ): Int

    /**
     * 특정 게시물의 일별 댓글수를 원자적으로 1 감소시킵니다.
     * 날짜가 다른 경우(어제 작성 → 오늘 삭제) 음수 값을 가질 수 있습니다.
     *
     * @param postId 게시물 ID
     * @param statDate 통계 날짜
     * @return 업데이트된 행 수 (0이면 해당 날짜 레코드 미존재)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE post_daily_stats
        SET comment_count = comment_count - 1, updated_at_utc = NOW()
        WHERE post_id = :postId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun decrementCommentCount(
        @Param("postId") postId: UUID,
        @Param("statDate") statDate: LocalDate,
    ): Int
}
