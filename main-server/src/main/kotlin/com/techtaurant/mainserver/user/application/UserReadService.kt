package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.user.dto.UserResponse
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserReadService(
    private val userRepository: UserRepository
) {

    fun getMe(userId : UUID): UserResponse {
        // SecurityContext에서 userId를 받아 DB에서 User 조회
        val user = userRepository.findById(userId).orElseThrow {
            ApiException(UserStatus.ID_NOT_FOUND)
        }
        return UserResponse.from(user)
    }

}
