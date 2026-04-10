package com.techtaurant.mainserver.user.application

import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.DataIntegrityViolationException

class UserUniqueNameServiceTest {
    private val userRepository: UserRepository = mockk()
    private lateinit var userUniqueNameService: UserUniqueNameService

    @BeforeEach
    fun setUp() {
        userUniqueNameService = UserUniqueNameService(userRepository)
    }

    @Test
    @DisplayName("닉네임이 유니크하면 원래 이름으로 바로 저장한다")
    fun `save with original name when user name is unique`() {
        val attemptedNames = mutableListOf<String>()

        every { userRepository.save(any()) } answers {
            val user = firstArg<User>()
            attemptedNames += user.name
            user
        }

        val savedUser =
            userUniqueNameService.saveNewUser(
                User(
                    name = "unique-name",
                    email = "unique@test.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "google-identifier",
                    role = UserRole.USER,
                    profileImageUrl = "",
                ),
            )

        assertEquals(listOf("unique-name"), attemptedNames)
        assertEquals("unique-name", savedUser.name)
    }

    @Test
    @DisplayName("닉네임 유니크 제약 충돌 시 랜덤 닉네임으로 재시도한다")
    fun `retry with random name when duplicate user name occurs`() {
        val attemptedNames = mutableListOf<String>()

        every { userRepository.save(any()) } answers {
            val user = firstArg<User>()
            attemptedNames += user.name

            if (attemptedNames.size == 1) {
                throw DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_users_name\"")
            }

            user
        }

        val savedUser =
            userUniqueNameService.saveNewUser(
                User(
                    name = "duplicate-name",
                    email = "duplicate@test.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "google-identifier",
                    role = UserRole.USER,
                    profileImageUrl = "",
                ),
            )

        assertEquals(2, attemptedNames.size)
        assertEquals("duplicate-name", attemptedNames.first())
        assertNotEquals("duplicate-name", attemptedNames.last())
        assertEquals(12, attemptedNames.last().length)
        assertEquals(attemptedNames.last(), savedUser.name)
    }

    @Test
    @DisplayName("다른 유니크 제약 충돌은 재시도하지 않고 그대로 예외를 던진다")
    fun `rethrow when other unique constraint occurs`() {
        every { userRepository.save(any()) } throws
            DataIntegrityViolationException("duplicate key value violates unique constraint \"users_identifier_provider_key\"")

        val exception =
            assertThrows<DataIntegrityViolationException> {
                userUniqueNameService.saveNewUser(
                    User(
                        name = "duplicate-name",
                        email = "duplicate@test.com",
                        provider = OAuthProvider.GOOGLE,
                        identifier = "google-identifier",
                        role = UserRole.USER,
                        profileImageUrl = "",
                    ),
                )
            }

        assertTrue(exception.message!!.contains("users_identifier_provider_key"))
    }
}
