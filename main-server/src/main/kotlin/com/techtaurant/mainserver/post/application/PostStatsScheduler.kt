package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 게시물 통계를 실시간으로 집계하는 스케줄러입니다.
 * view log, like log를 기반으로 posts 테이블의 캐시 컬럼(viewCount, likeCount, commentCount)을 업데이트합니다.
 * 매일 정해진 시간에 실행되어 어제의 통계를 post_daily_stats 테이블에 기록합니다.
 */
@Component
class PostStatsScheduler(
    private val postStatsService: PostStatsService,
    private val postRepository: PostRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 매일 자정에 실행되는 일일 통계 집계 작업입니다.
     * 모든 게시물의 어제 통계를 post_daily_stats 테이블에 저장하고,
     * statsUpdatedAt을 현재 시간으로 갱신하여 다음 실행 때 업데이트할 대상을 명확히 합니다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @SchedulerLock(
        name = "aggregateDailyStats",
        lockAtMostFor = "30m",
        lockAtLeastFor = "5m"
    )
    @Transactional
    fun aggregateDailyStats() {
        try {
            logger.info("일일 게시물 통계 집계 시작")
            postStatsService.aggregateDailyPostStats()
            logger.info("일일 게시물 통계 집계 완료")
        } catch (e: Exception) {
            logger.error("일일 게시물 통계 집계 실패", e)
        }
    }

    /**
     * 지정된 주기로 모든 게시물의 실시간 통계를 갱신합니다.
     * view log, like log, comment log 등의 이벤트에서 수집한 데이터를
     * posts 테이블의 viewCount, likeCount, commentCount 캐시 컬럼에 반영합니다.
     *
     * 실제 구현에서는 별도의 로그 테이블(post_view_log, post_like_log 등)에서
     * 통계 데이터를 집계하여 업데이트합니다.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000) // 60초마다 실행, 초기 지연 30초
    @SchedulerLock(
        name = "updateRealtimeStats",
        lockAtMostFor = "2m",
        lockAtLeastFor = "55s"
    )
    @Transactional
    fun updateRealtimeStats() {
        try {
            logger.debug("실시간 게시물 통계 갱신 시작")

            val allPosts = postRepository.findAll()
            allPosts.forEach { post ->
                updatePostStats(post)
            }

            logger.debug("실시간 게시물 통계 갱신 완료")
        } catch (e: Exception) {
            logger.error("실시간 게시물 통계 갱신 실패", e)
        }
    }

    /**
     * 특정 게시물의 통계를 갱신합니다.
     *
     * 실제 구현에서는 다음과 같은 로그 테이블에서 데이터를 수집합니다:
     * - post_view_log: 조회 이벤트 로그
     * - post_like_log: 좋아요 이벤트 로그
     * - post_comment_log: 댓글 이벤트 로그
     *
     * @param post 업데이트할 게시물
     */
    private fun updatePostStats(post: Post) {
        // TODO: view log에서 최신 viewCount 조회
        // val latestViewCount = postViewLogRepository.countByPostId(post.id!!)

        // TODO: like log에서 최신 likeCount 조회
        // val latestLikeCount = postLikeLogRepository.countByPostId(post.id!!)

        // TODO: comment log에서 최신 commentCount 조회
        // val latestCommentCount = postCommentLogRepository.countByPostId(post.id!!)

        // TODO: 통계 값이 변경되었으면 post 엔티티 업데이트
        // post.viewCount = latestViewCount
        // post.likeCount = latestLikeCount
        // post.commentCount = latestCommentCount
        // postRepository.save(post)
    }
}
