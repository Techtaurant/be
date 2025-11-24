package com.techtaurant.mainserver.security.oauth.service

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.security.oauth.CustomOAuth2User
import com.techtaurant.mainserver.security.oauth.info.GoogleOAuth2UserInfo
import com.techtaurant.mainserver.security.oauth.info.OAuth2UserInfo
import com.techtaurant.mainserver.security.oauth.status.OAuthStatus
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
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId

        val oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.attributes)
        val provider = getOAuthProvider(registrationId)

        val user = userRepository.findByIdentifierAndProvider(oAuth2UserInfo.getId(), provider)
            ?: createUser(oAuth2UserInfo, provider)

        return CustomOAuth2User(user, oAuth2User.attributes)
    }

    private fun getOAuth2UserInfo(registrationId: String, attributes: Map<String, Any>): OAuth2UserInfo {
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

    private fun createUser(userInfo: OAuth2UserInfo, provider: OAuthProvider): User {
        val user = User(
            name = userInfo.getName(),
            email = userInfo.getEmail(),
            provider = provider,
            identifier = userInfo.getId(),
            role = UserRole.USER,
            profileImageUrl = userInfo.getProfileImageUrl(),
        )
        return userRepository.save(user)
    }
}
