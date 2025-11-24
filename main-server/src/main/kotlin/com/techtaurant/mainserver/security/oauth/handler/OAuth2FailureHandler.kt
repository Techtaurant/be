package com.techtaurant.mainserver.security.oauth.handler

import com.techtaurant.mainserver.security.oauth.status.OAuthStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2FailureHandler(
    @Value("\${oauth2.redirect.failure-url}") private val failureRedirectUrl: String,
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val status = OAuthStatus.OAUTH_AUTHENTICATION_FAILED

        val redirectUrl = UriComponentsBuilder.fromUriString(failureRedirectUrl)
            .queryParam("error", status.getCustomStatusCode())
            .queryParam("message", status.getDescription())
            .build()
            .toUriString()

        response.sendRedirect(redirectUrl)
    }
}
