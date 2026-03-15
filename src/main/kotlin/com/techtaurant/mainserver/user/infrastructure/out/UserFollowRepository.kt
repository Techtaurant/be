package com.techtaurant.mainserver.user.infrastructure.out

import com.techtaurant.mainserver.user.entity.UserFollow
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserFollowRepository : JpaRepository<UserFollow, UUID> {
    fun findByFollowerIdAndFollowingId(
        followerId: UUID,
        followingId: UUID,
    ): UserFollow?

    fun countByFollowerId(followerId: UUID): Long

    fun countByFollowingId(followingId: UUID): Long

    fun findAllByFollowerIdOrderByCreatedAtDesc(followerId: UUID): List<UserFollow>

    fun findAllByFollowingIdOrderByCreatedAtDesc(followingId: UUID): List<UserFollow>

    @Modifying
    @Transactional
    @Query(
        """
        DELETE FROM UserFollow uf
        WHERE (uf.follower.id = :firstUserId AND uf.following.id = :secondUserId)
           OR (uf.follower.id = :secondUserId AND uf.following.id = :firstUserId)
        """,
    )
    fun deleteMutualFollows(
        @Param("firstUserId") firstUserId: UUID,
        @Param("secondUserId") secondUserId: UUID,
    ): Int
}
