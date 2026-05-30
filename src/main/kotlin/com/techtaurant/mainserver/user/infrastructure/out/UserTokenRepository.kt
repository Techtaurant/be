package com.techtaurant.mainserver.user.infrastructure.out

import com.techtaurant.mainserver.user.entity.UserToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserTokenRepository : JpaRepository<UserToken, UUID> {
    fun existsByUserIdAndTokenHash(
        userId: UUID,
        tokenHash: String,
    ): Boolean

    fun deleteAllByUserId(userId: UUID): Long
}
