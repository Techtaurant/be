package com.techtaurant.mainserver.user.infrastructure.out

import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByIdentifierAndProvider(identifier: String, provider: OAuthProvider): User?
}
