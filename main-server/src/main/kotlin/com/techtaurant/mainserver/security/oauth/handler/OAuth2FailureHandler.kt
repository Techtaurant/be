package com.techtaurant.mainserver.security.oauth.handler

import com.techtaurant.mainserver.security.oauth.repository.HttpCookieOAuth2AuthorizationRequestRepository
import com.techtaurant.mainserver.security.oauth.status.OAuthStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2FailureHandler(
    private val cookieOAuth2AuthorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository,
    @param:Value("\${oauth2.redirect.failure-url}") private val failureRedirectUrl: String,
) : AuthenticationFailureHandler {
    private val logger = LoggerFactory.getLogger(OAuth2FailureHandler::class.java)

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val clientIp =
            request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                ?: request.remoteAddr
        val oauthProvider = extractOAuthProvider(request.requestURI)

        logger.error(
            "OAuth2 authentication failed: provider={}, error={}, clientIp={}",
            oauthProvider,
            exception.message,
            clientIp,
        )

        val status = OAuthStatus.OAUTH_AUTHENTICATION_FAILED

        val redirectUrl =
            UriComponentsBuilder.fromUriString(failureRedirectUrl)
                .queryParam("error", status.getCustomStatusCode())
                .queryParam("message", status.getDescription())
                .build()
                .encode()
                .toUriString()

        // OAuth2 인증 실패 후 authorization request 쿠키 정리
        cookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(response)

        response.sendRedirect(redirectUrl)
    }

    /**
     * Request URI에서 OAuth provider 이름을 추출한다.
     * 예: /oauth2/callback/google -> google
     */
    private fun extractOAuthProvider(requestUri: String): String {
        return requestUri.split("/").lastOrNull { it.isNotBlank() } ?: "unknown"
    }
}
