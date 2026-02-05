package com.techtaurant.mainserver.post.infrastructure.out

import com.techtaurant.mainserver.post.entity.PostViewLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface PostViewLogRepository : JpaRepository<PostViewLog, UUID> {
    /**
     * 특정 게시글의 총 조회수를 조회합니다.
     *
     * @param postId 게시글 ID
     * @return 총 조회수
     */
    fun countByPostId(postId: UUID): Long

    /**
     * 특정 시점 이후 이벤트가 발생한 게시글 ID 목록을 조회합니다.
     * statsUpdatedAt 이후 조회 이벤트가 발생한 게시글만 반환하여 효율적인 통계 갱신을 지원합니다.
     *
     * @param since 이 시점 이후의 이벤트만 조회
     * @return 이벤트가 발생한 게시글 ID 목록
     */
    @Query(
        """
        SELECT DISTINCT pvl.post.id
        FROM PostViewLog pvl
        WHERE pvl.createdAt > :since
        """,
    )
    fun findDistinctPostIdsByCreatedAtAfter(
        @Param("since") since: Date,
    ): List<UUID>

    /**
     * 특정 사용자가 조회한 게시글 ID 목록을 조회합니다.
     * 게시물 목록에서 사용자가 읽은 게시물을 표시하기 위해 사용됩니다.
     *
     * @param userId 사용자 ID
     * @param postIds 확인할 게시글 ID 목록
     * @return 사용자가 조회한 게시글 ID 목록
     */
    @Query(
        """
        SELECT DISTINCT pvl.post.id
        FROM PostViewLog pvl
        WHERE pvl.user.id = :userId
        AND pvl.post.id IN :postIds
        """,
    )
    fun findDistinctPostIdsByUserIdAndPostIdIn(
        @Param("userId") userId: UUID,
        @Param("postIds") postIds: List<UUID>,
    ): List<UUID>
}
