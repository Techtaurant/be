package com.techtaurant.mainserver.security.oauth.handler

import com.techtaurant.mainserver.security.config.CorsProperties
import com.techtaurant.mainserver.security.helper.CookieHelper
import com.techtaurant.mainserver.security.oauth.repository.HttpCookieOAuth2AuthorizationRequestRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * OAuth2 인증 완료 후 리다이렉트할 URL을 결정한다.
 * 쿠키에 저장된 요청 origin을 기반으로 리다이렉트 URL을 생성하며,
 * CORS 허용 목록에 포함된 origin만 허용하여 오픈 리다이렉트 공격을 방지한다.
 *
 * @param request 현재 HTTP 요청
 * @param path 리다이렉트 경로 (예: /oauth/callback)
 * @return 완성된 리다이렉트 URL
 */
@Component
class OAuth2RedirectResolver(
    private val cookieHelper: CookieHelper,
    private val corsProperties: CorsProperties,
) {
    private val logger = LoggerFactory.getLogger(OAuth2RedirectResolver::class.java)

    companion object {
        const val SUCCESS_PATH = "/oauth/callback"
        const val FAILURE_PATH = "/oauth/error"
    }

    fun resolve(
        request: HttpServletRequest,
        path: String,
    ): String {
        val origin =
            cookieHelper.getCookie(
                request,
                HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_ORIGIN_COOKIE,
            )

        val allowedOrigins =
            corsProperties.allowedOrigins
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        logger.info(
            "resolve: originCookie={}, allowedOrigins={}, path={}",
            origin,
            allowedOrigins,
            path,
        )

        val allCookieNames = request.cookies?.map { it.name } ?: emptyList()
        logger.info("resolve: allCookies={}", allCookieNames)

        val validOrigin =
            if (origin != null && origin in allowedOrigins) {
                origin
            } else {
                logger.warn(
                    "resolve: origin not in allowedOrigins, falling back. origin={}, allowedOrigins={}",
                    origin,
                    allowedOrigins,
                )
                allowedOrigins.firstOrNull() ?: "http://localhost:3000"
            }

        val redirectUrl = "$validOrigin$path"
        logger.info("resolve: redirectUrl={}", redirectUrl)
        return redirectUrl
    }
}
