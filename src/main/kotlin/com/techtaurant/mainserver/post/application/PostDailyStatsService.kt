package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.post.entity.PostDailyStats
import com.techtaurant.mainserver.post.infrastructure.out.PostDailyStatsRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
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
    fun incrementViewCount(
        postId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(postId, statDate, postDailyStatsRepository::incrementViewCount)
    }

    /**
     * 일별 좋아요수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     */
    fun incrementLikeCount(
        postId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(postId, statDate, postDailyStatsRepository::incrementLikeCount)
    }

    /**
     * 일별 좋아요수를 원자적으로 1 감소시킵니다.
     * 레코드가 없으면 생성 후 감소를 재시도하여 음수 값을 가질 수 있습니다.
     *
     * @param postId 게시물 ID
     */
    fun decrementLikeCount(
        postId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(postId, statDate, postDailyStatsRepository::decrementLikeCount)
    }

    /**
     * 일별 댓글수를 원자적으로 1 증가시킵니다.
     *
     * @param postId 게시물 ID
     */
    fun incrementCommentCount(
        postId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(postId, statDate, postDailyStatsRepository::incrementCommentCount)
    }

    /**
     * 지정한 일자의 댓글수를 원자적으로 1 감소시킵니다.
     * 레코드가 없으면 생성 후 감소를 재시도하여 음수 값을 가질 수 있습니다.
     *
     * @param postId 게시물 ID
     * @param statDate 통계 일자
     */
    fun decrementCommentCount(
        postId: UUID,
        statDate: LocalDate,
    ) {
        applyDailyStatsChange(postId, statDate, postDailyStatsRepository::decrementCommentCount)
    }

    private fun applyDailyStatsChange(
        postId: UUID,
        statDate: LocalDate,
        changeFn: (UUID, LocalDate) -> Int,
    ) {
        if (changeFn(postId, statDate) == 0) {
            retryDailyStatsChangeAfterCreate(postId, statDate, changeFn)
        }
    }

    /**
     * DailyStats 레코드가 없으면 생성한 뒤 변경 쿼리를 다시 실행합니다.
     * Unique Constraint 위반 시에는 레코드 생성만 건너뛰고 변경 쿼리만 재시도합니다.
     */
    private fun retryDailyStatsChangeAfterCreate(
        postId: UUID,
        statDate: LocalDate,
        changeFn: (UUID, LocalDate) -> Int,
    ) {
        try {
            createDailyStats(postId, statDate)
        } catch (e: DataIntegrityViolationException) {
            logger.debug("DailyStats 동시 생성 감지, 변경 쿼리 재시도: postId={}", postId)
        }
        changeFn(postId, statDate)
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
