package com.techtaurant.mainserver.security.oauth.service

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.oauth.CustomOAuth2User
import com.techtaurant.mainserver.security.oauth.info.GoogleOAuth2UserInfo
import com.techtaurant.mainserver.security.oauth.info.OAuth2UserInfo
import com.techtaurant.mainserver.security.oauth.status.OAuthStatus
import com.techtaurant.mainserver.user.application.UserUniqueNameService
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val userUniqueNameService: UserUniqueNameService,
) : DefaultOAuth2UserService() {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        try {
            val oAuth2User = fetchOAuth2User(userRequest)
            val registrationId = userRequest.clientRegistration.registrationId

            val oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.attributes)
            val provider = getOAuthProvider(registrationId)

            val user =
                userRepository.findByIdentifierAndProvider(oAuth2UserInfo.getId(), provider)
                    ?: createUser(oAuth2UserInfo, provider)

            return CustomOAuth2User(user, oAuth2User.attributes)
        } catch (e: Exception) {
            println("OAuth2 loadUser failed - ClientRegistration: ${userRequest.clientRegistration.registrationId}")
            println("Exception: ${e.message}")
            println("Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            throw e
        }
    }

    protected open fun fetchOAuth2User(userRequest: OAuth2UserRequest): OAuth2User = super.loadUser(userRequest)

    private fun getOAuth2UserInfo(
        registrationId: String,
        attributes: Map<String, Any>,
    ): OAuth2UserInfo {
        return when (registrationId) {
            "google" -> GoogleOAuth2UserInfo(attributes)
            else -> throw ApiException(OAuthStatus.OAUTH_PROVIDER_NOT_SUPPORTED)
        }
    }

    private fun getOAuthProvider(registrationId: String): OAuthProvider {
        return when (registrationId) {
            "google" -> OAuthProvider.GOOGLE
            else -> throw ApiException(OAuthStatus.OAUTH_PROVIDER_NOT_SUPPORTED)
        }
    }

    private fun createUser(
        userInfo: OAuth2UserInfo,
        provider: OAuthProvider,
    ): User {
        return userUniqueNameService.saveNewUser(
            User(
                name = userInfo.getName(),
                email = userInfo.getEmail(),
                provider = provider,
                identifier = userInfo.getId(),
                role = UserRole.USER,
                profileImageUrl = userInfo.getProfileImageUrl(),
            ),
        )
    }
}
