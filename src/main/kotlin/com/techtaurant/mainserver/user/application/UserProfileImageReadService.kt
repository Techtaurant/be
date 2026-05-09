package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.user.dto.UserProfileImageResponse
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserProfileImageReadService(
    private val userRepository: UserRepository,
    private val userProfileImageResolver: UserProfileImageResolver,
) {
    fun getUserProfileImages(userIds: List<UUID>): List<UserProfileImageResponse> {
        val normalizedUserIds = userIds.distinct()
        if (normalizedUserIds.isEmpty()) {
            return emptyList()
        }

        val userById = userRepository.findAllById(normalizedUserIds).associateBy { it.id!! }
        val users = normalizedUserIds.mapNotNull { userById[it] }
        val profileImageUrlByUserId = userProfileImageResolver.resolve(users)

        return users.map { user ->
            val userId = user.id!!
            UserProfileImageResponse(
                userId = userId,
                profileImageUrl = profileImageUrlByUserId[userId] ?: user.getFallbackProfileImageUrl(),
            )
        }
    }
}
