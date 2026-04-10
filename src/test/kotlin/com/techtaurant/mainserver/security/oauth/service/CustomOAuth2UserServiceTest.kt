package com.techtaurant.mainserver.security.oauth.service

import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.oauth.CustomOAuth2User
import com.techtaurant.mainserver.user.application.UserUniqueNameService
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import java.time.Instant

class CustomOAuth2UserServiceTest {
    private val userRepository: UserRepository = mockk()
    private val userUniqueNameService: UserUniqueNameService = mockk()
    private lateinit var customOAuth2UserService: TestableCustomOAuth2UserService

    @BeforeEach
    fun setUp() {
        customOAuth2UserService = TestableCustomOAuth2UserService(userRepository, userUniqueNameService)
    }

    @Test
    @DisplayName("OAuth 신규 가입 시 유니크 닉네임 정책으로 사용자를 생성한다")
    fun `create oauth user with unique name policy`() {
        val attributes =
            mapOf(
                "sub" to "google-sub-id",
                "email" to "tech@test.com",
                "name" to "중복닉네임",
                "picture" to "https://example.com/profile.png",
            )
        val createdUser =
            User(
                name = "randomname12",
                email = "tech@test.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "google-sub-id",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/profile.png",
            )
        val request = createOAuth2UserRequest()

        customOAuth2UserService.oauth2User =
            DefaultOAuth2User(
                listOf(SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub",
            )

        every { userRepository.findByIdentifierAndProvider("google-sub-id", OAuthProvider.GOOGLE) } returns null
        every { userUniqueNameService.saveNewUser(any()) } returns createdUser

        val result = customOAuth2UserService.loadUser(request) as CustomOAuth2User

        assertEquals(createdUser, result.getUser())
        assertEquals("randomname12", result.name)
        verify(exactly = 1) {
            userUniqueNameService.saveNewUser(
                withArg {
                    assertEquals("중복닉네임", it.name)
                    assertEquals("tech@test.com", it.email)
                },
            )
        }
    }

    private fun createOAuth2UserRequest(): OAuth2UserRequest {
        val clientRegistration =
            ClientRegistration
                .withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build()

        val accessToken =
            OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
            )

        return OAuth2UserRequest(clientRegistration, accessToken)
    }

    private class TestableCustomOAuth2UserService(
        userRepository: UserRepository,
        userUniqueNameService: UserUniqueNameService,
    ) : CustomOAuth2UserService(userRepository, userUniqueNameService) {
        lateinit var oauth2User: OAuth2User

        override fun fetchOAuth2User(userRequest: OAuth2UserRequest): OAuth2User = oauth2User
    }
}
