package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.user.dto.UserResponse
import com.techtaurant.mainserver.user.entity.User
import org.springframework.stereotype.Component

@Component
class UserResponseAssembler(
    private val userProfileImageResolver: UserProfileImageResolver,
) {
    fun assemble(user: User): UserResponse {
        return assemble(listOf(user)).first()
    }

    fun assemble(users: List<User>): List<UserResponse> {
        if (users.isEmpty()) {
            return emptyList()
        }

        val profileImageUrlByUserId = userProfileImageResolver.resolve(users)

        return users.map { user ->
            val profileImageUrl = user.id?.let { profileImageUrlByUserId[it] } ?: user.getFallbackProfileImageUrl()
            UserResponse.from(user, profileImageUrl)
        }
    }
}
