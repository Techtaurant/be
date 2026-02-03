package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostLikeLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostViewLogRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 게시물 통계를 실시간으로 집계하는 스케줄러입니다.
 * view log, like log를 기반으로 posts 테이블의 캐시 컬럼(viewCount, likeCount, commentCount)을 업데이트합니다.
 * 매일 정해진 시간에 실행되어 어제의 통계를 post_daily_stats 테이블에 기록합니다.
 */
@Component
class PostStatsScheduler(
    private val postStatsService: PostStatsService,
    private val postRepository: PostRepository,
    private val postViewLogRepository: PostViewLogRepository,
    private val postLikeLogRepository: PostLikeLogRepository,
    private val commentRepository: CommentRepository,
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
     * 지정된 주기로 이벤트가 발생한 게시물의 실시간 통계를 갱신합니다.
     * view log, like log, comment 테이블에서 수집한 데이터를
     * posts 테이블의 viewCount, likeCount, commentCount 캐시 컬럼에 반영합니다.
     *
     * statsUpdatedAt을 기준으로 해당 시점 이후 이벤트가 발생한 게시물만 조회하여
     * 효율적으로 통계를 갱신합니다.
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

            // 현재 시점을 기준으로 통계 갱신 시작
            val now = Date()

            // 이벤트가 발생한 게시물 ID 목록 수집
            val postsWithEvents = collectPostsWithEvents()

            if (postsWithEvents.isEmpty()) {
                logger.debug("갱신할 게시물이 없습니다")
                return
            }

            logger.debug("갱신 대상 게시물 수: ${postsWithEvents.size}")

            // 각 게시물의 통계를 갱신
            postsWithEvents.forEach { postId ->
                try {
                    updatePostStats(postId, now)
                } catch (e: Exception) {
                    logger.error("게시물 통계 갱신 실패: postId=$postId", e)
                }
            }

            logger.debug("실시간 게시물 통계 갱신 완료")
        } catch (e: Exception) {
            logger.error("실시간 게시물 통계 갱신 실패", e)
        }
    }

    /**
     * 이벤트가 발생한 게시물 ID 목록을 수집합니다.
     * statsUpdatedAt 이후 조회, 좋아요, 댓글 이벤트가 발생한 게시물을 찾습니다.
     *
     * @return 이벤트가 발생한 게시물 ID 목록 (중복 제거)
     */
    private fun collectPostsWithEvents(): Set<UUID> {
        val postsWithEvents = mutableSetOf<UUID>()

        // 마지막 통계 갱신 시점 기준 (기본값: 2분 전)
        val twoMinutesAgo = Date(System.currentTimeMillis() - 120_000)

        // 조회 이벤트가 발생한 게시물
        val postsWithViews = postViewLogRepository.findDistinctPostIdsByCreatedAtAfter(twoMinutesAgo)
        postsWithEvents.addAll(postsWithViews)

        // 좋아요 이벤트가 발생한 게시물
        val postsWithLikes = postLikeLogRepository.findDistinctPostIdsByUpdatedAtAfter(twoMinutesAgo)
        postsWithEvents.addAll(postsWithLikes)

        // 댓글이 작성/수정된 게시물 (Comment 테이블에서 직접 조회)
        // 실제 구현에서는 Comment의 updatedAt을 기준으로 조회하는 쿼리를 CommentRepository에 추가 필요
        // 현재는 View/Like 이벤트만 처리하고, 댓글은 일별 집계에서 처리

        return postsWithEvents
    }

    /**
     * 특정 게시물의 통계를 갱신합니다.
     * 각 이벤트 로그에서 통계를 집계하고 posts 테이블의 캐시 컬럼을 업데이트합니다.
     *
     * @param postId 업데이트할 게시물 ID
     * @param now 통계 갱신 시점
     */
    private fun updatePostStats(postId: UUID, now: Date) {
        val post = postRepository.findById(postId).orElse(null) ?: run {
            logger.warn("게시물을 찾을 수 없습니다: postId=$postId")
            return
        }

        // view log에서 최신 viewCount 조회
        val latestViewCount = postViewLogRepository.countByPostId(postId)

        // like log에서 최신 likeCount 조회 (isLiked = TRUE인 것만)
        val latestLikeCount = postLikeLogRepository.countByPostIdAndIsLikedTrue(postId)

        // comment 테이블에서 최신 commentCount 조회
        val latestCommentCount = commentRepository.countByPostId(postId)

        // 통계 값이 변경되었으면 post 엔티티 업데이트
        var updated = false
        if (post.viewCount != latestViewCount) {
            post.viewCount = latestViewCount
            updated = true
        }
        if (post.likeCount != latestLikeCount) {
            post.likeCount = latestLikeCount
            updated = true
        }
        if (post.commentCount != latestCommentCount) {
            post.commentCount = latestCommentCount
            updated = true
        }

        if (updated) {
            post.statsUpdatedAt = now
            postRepository.save(post)
            logger.debug(
                "게시물 통계 갱신: postId={}, viewCount={}, likeCount={}, commentCount={}",
                postId,
                latestViewCount,
                latestLikeCount,
                latestCommentCount
            )
        }
    }
}
