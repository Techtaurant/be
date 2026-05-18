package com.techtaurant.mainserver.link.infrastructure.out

import com.techtaurant.mainserver.link.entity.LinkDailyStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.sql.Date
import java.util.UUID

interface LinkDailyStatsRepository : JpaRepository<LinkDailyStats, UUID> {
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        UPDATE link_daily_stats
        SET view_count = view_count + 1, updated_at = NOW()
        WHERE link_id = :linkId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun incrementViewCount(
        @Param("linkId") linkId: UUID,
        @Param("statDate") statDate: Date,
    ): Int

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        UPDATE link_daily_stats
        SET like_count = like_count + 1, updated_at = NOW()
        WHERE link_id = :linkId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun incrementLikeCount(
        @Param("linkId") linkId: UUID,
        @Param("statDate") statDate: Date,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE link_daily_stats
        SET like_count = like_count - 1, updated_at = NOW()
        WHERE link_id = :linkId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun decrementLikeCount(
        @Param("linkId") linkId: UUID,
        @Param("statDate") statDate: Date,
    ): Int

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
        """
        UPDATE link_daily_stats
        SET save_count = save_count + 1, updated_at = NOW()
        WHERE link_id = :linkId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun incrementSaveCount(
        @Param("linkId") linkId: UUID,
        @Param("statDate") statDate: Date,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE link_daily_stats
        SET save_count = save_count - 1, updated_at = NOW()
        WHERE link_id = :linkId AND stat_date = :statDate
        """,
        nativeQuery = true,
    )
    fun decrementSaveCount(
        @Param("linkId") linkId: UUID,
        @Param("statDate") statDate: Date,
    ): Int
}
