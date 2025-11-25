package com.techtaurant.mainserver.security.oauth.handler

import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.security.cookie.CookieHelper
import com.techtaurant.mainserver.security.jwt.JwtConstants
import com.techtaurant.mainserver.security.jwt.JwtProperties
import com.techtaurant.mainserver.security.jwt.JwtTokenProvider
import com.techtaurant.mainserver.security.oauth.CustomOAuth2User
import com.techtaurant.mainserver.user.enums.UserStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties,
    private val cookieHelper: CookieHelper,
    @param:Value("\${oauth2.redirect.success-url}") private val successRedirectUrl: String,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val customOAuth2User = authentication.principal as CustomOAuth2User
        val user = customOAuth2User.getUser()
        val userId = user.id ?: throw ApiException(UserStatus.ID_NOT_FOUND)

        val accessToken = jwtTokenProvider.createAccessToken(userId)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)

        cookieHelper.addCookie(
            response,
            JwtConstants.ACCESS_TOKEN_COOKIE,
            accessToken,
            (jwtProperties.accessTokenExpiration / 1000).toInt()
        )
        cookieHelper.addCookie(
            response,
            JwtConstants.REFRESH_TOKEN_COOKIE,
            refreshToken,
            (jwtProperties.refreshTokenExpiration / 1000).toInt()
        )

        response.sendRedirect(successRedirectUrl)
    }
}
