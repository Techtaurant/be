package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostDailyStats
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

/**
 * 일별 게시물 통계 증분 서비스
 * 이벤트 발생 시 PostDailyStats 레코드를 생성하거나 원자적으로 증분합니다.
 * Unique Constraint(post_id, stat_date) 위반 시 재시도하여 동시성을 안전하게 처리합니다.
 */
@Service
class PostDailyStatsService(
    private val postDailyStatsRepository: PostDailyStatsRepository,
    private val postRepository: PostRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 일별 조회수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun incrementViewCount(postId: UUID) {
        val today = LocalDate.now()
        val updatedRows = postDailyStatsRepository.incrementViewCount(postId, today)
        if (updatedRows == 0) {
            createDailyStatsAndIncrement(postId, today) { id, date ->
                postDailyStatsRepository.incrementViewCount(id, date)
            }
        }
    }

    /**
     * 일별 좋아요수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun incrementLikeCount(postId: UUID) {
        val today = LocalDate.now()
        val updatedRows = postDailyStatsRepository.incrementLikeCount(postId, today)
        if (updatedRows == 0) {
            createDailyStatsAndIncrement(postId, today) { id, date ->
                postDailyStatsRepository.incrementLikeCount(id, date)
            }
        }
    }

    /**
     * 일별 좋아요수를 원자적으로 1 감소시킵니다.
     *
     * @param postId 게시물 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun decrementLikeCount(postId: UUID) {
        val today = LocalDate.now()
        val updatedRows = postDailyStatsRepository.decrementLikeCount(postId, today)
        if (updatedRows == 0) {
            createDailyStats(postId, today)
        }
    }

    /**
     * 일별 댓글수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun incrementCommentCount(postId: UUID) {
        val today = LocalDate.now()
        val updatedRows = postDailyStatsRepository.incrementCommentCount(postId, today)
        if (updatedRows == 0) {
            createDailyStatsAndIncrement(postId, today) { id, date ->
                postDailyStatsRepository.incrementCommentCount(id, date)
            }
        }
    }

    /**
     * DailyStats 레코드를 생성하고 증분 쿼리를 재실행합니다.
     * Unique Constraint 위반 시 (동시 insert) 증분 쿼리만 재실행합니다.
     */
    private fun createDailyStatsAndIncrement(
        postId: UUID,
        statDate: LocalDate,
        incrementFn: (UUID, LocalDate) -> Int,
    ) {
        try {
            createDailyStats(postId, statDate)
        } catch (e: DataIntegrityViolationException) {
            logger.debug("DailyStats 동시 생성 감지, 증분 재시도: postId={}", postId)
        }
        incrementFn(postId, statDate)
    }

    /**
     * 해당 날짜의 DailyStats 레코드를 새로 생성합니다.
     */
    private fun createDailyStats(
        postId: UUID,
        statDate: LocalDate,
    ) {
        val post = postRepository.getReferenceById(postId)
        val dailyStats = PostDailyStats(post = post, statDate = statDate)
        postDailyStatsRepository.save(dailyStats)
    }
}
