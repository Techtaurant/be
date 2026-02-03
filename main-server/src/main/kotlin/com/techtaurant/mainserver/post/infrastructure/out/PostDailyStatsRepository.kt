package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.PostDailyStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface PostDailyStatsRepository : JpaRepository<PostDailyStats, UUID> {

    /**
     * 특정 게시물들의 전체 통계 합계 조회 (동기화용)
     * 반환 배열: [postId, sumViewCount, sumLikeCount, sumCommentCount]
     *
     * @param postIds 조회할 게시물 ID 목록
     */
    @Query("""
        SELECT pds.post.id, SUM(pds.viewCount), SUM(pds.likeCount), SUM(pds.commentCount)
        FROM PostDailyStats pds
        WHERE pds.post.id IN :postIds
        GROUP BY pds.post.id
    """)
    fun findStatsSumByPostIds(postIds: List<UUID>): List<Array<Any>>
}
