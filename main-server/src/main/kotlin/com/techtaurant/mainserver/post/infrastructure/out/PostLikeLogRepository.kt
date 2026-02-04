package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.PostLikeLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface PostLikeLogRepository : JpaRepository<PostLikeLog, UUID> {

    /**
     * 특정 게시글의 현재 좋아요 수를 조회합니다.
     * isLiked가 TRUE인 레코드만 카운트합니다.
     *
     * @param postId 게시글 ID
     * @return 좋아요 수
     */
    fun countByPostIdAndIsLikedTrue(postId: UUID): Long

    /**
     * 특정 게시글에 대한 특정 사용자의 좋아요 로그를 조회합니다.
     * 기존 로그가 있는지 확인하여 UPDATE 또는 INSERT를 결정하는 데 사용됩니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 좋아요 로그 (없으면 null)
     */
    fun findByPostIdAndUserId(postId: UUID, userId: UUID): PostLikeLog?

    /**
     * 특정 시점 이후 이벤트가 발생한 게시글 ID 목록을 조회합니다.
     * statsUpdatedAt 이후 좋아요/취소 이벤트가 발생한 게시글만 반환하여 효율적인 통계 갱신을 지원합니다.
     *
     * @param since 이 시점 이후의 이벤트만 조회
     * @return 이벤트가 발생한 게시글 ID 목록
     */
    @Query(
        """
        SELECT DISTINCT pll.post.id
        FROM PostLikeLog pll
        WHERE pll.updatedAt > :since
        """
    )
    fun findDistinctPostIdsByUpdatedAtAfter(@Param("since") since: Date): List<UUID>
}
