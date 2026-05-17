package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.common.status.DefaultStatus
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class UserUniqueNameService(
    private val userRepository: UserRepository,
) {
    companion object {
        private const val MAX_SAVE_ATTEMPTS = 5
        private const val RANDOM_NAME_LENGTH = 12
        private const val USER_NAME_UNIQUE_CONSTRAINT = "uk_users_name"
        private val RANDOM_NAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
        private val secureRandom = SecureRandom()
    }

    fun saveNewUser(user: User): User {
        val userToSave = user

        repeat(MAX_SAVE_ATTEMPTS) { attempt ->
            try {
                return userRepository.save(userToSave)
            } catch (exception: DataIntegrityViolationException) {
                if (!isUserNameUniqueConstraintViolation(exception) || attempt == MAX_SAVE_ATTEMPTS - 1) {
                    throw exception
                }
                userToSave.name = generateRandomName()
            }
        }

        throw ApiException(DefaultStatus.SERVER_ERROR, "사용자 저장 재시도 횟수를 초과했습니다")
    }

    private fun isUserNameUniqueConstraintViolation(exception: DataIntegrityViolationException): Boolean {
        return generateSequence<Throwable>(exception) { current -> current.cause }
            .mapNotNull { throwable -> throwable.message }
            .any { message -> message.contains(USER_NAME_UNIQUE_CONSTRAINT, ignoreCase = true) }
    }

    private fun generateRandomName(): String {
        return buildString(RANDOM_NAME_LENGTH) {
            repeat(RANDOM_NAME_LENGTH) {
                append(RANDOM_NAME_CHARACTERS[secureRandom.nextInt(RANDOM_NAME_CHARACTERS.size)])
            }
        }
    }
}
