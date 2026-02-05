package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.PostDailyStats
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class PostStatsService(
    private val postRepository: PostRepository,
    private val postDailyStatsRepository: PostDailyStatsRepository,
) {
    /**
     * 모든 게시물의 실시간 통계를 기록합니다.
     * 매일 실행되어 어제의 뷰, 좋아요, 댓글 수를 일별 통계 테이블에 저장합니다.
     * statsUpdatedAt을 현재 시간으로 갱신하여 다음 집계 대상을 명확히 합니다.
     */
    fun aggregateDailyPostStats() {
        val yesterday = LocalDate.now().minusDays(1)
        val allPosts = postRepository.findAll()

        allPosts.forEach { post ->
            val existingStats = postDailyStatsRepository.findByPostIdAndStatDate(post.id!!, yesterday)

            if (existingStats != null) {
                // 이미 통계가 있으면 최신 값으로 업데이트
                existingStats.viewCount = post.viewCount
                existingStats.likeCount = post.likeCount
                existingStats.commentCount = post.commentCount
                postDailyStatsRepository.save(existingStats)
            } else {
                // 새로운 통계 레코드 생성
                val dailyStats =
                    PostDailyStats(
                        post = post,
                        statDate = yesterday,
                        viewCount = post.viewCount,
                        likeCount = post.likeCount,
                        commentCount = post.commentCount,
                    )
                postDailyStatsRepository.save(dailyStats)
            }
        }

        // 모든 게시물의 statsUpdatedAt을 현재 시간으로 갱신
        allPosts.forEach { post ->
            post.statsUpdatedAt = Date()
            postRepository.save(post)
        }
    }
}
