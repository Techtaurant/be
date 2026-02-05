package com.techtaurant.mainserver.security.oauth.repository

import com.techtaurant.mainserver.security.helper.CookieHelper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64

/**
 * 쿠키 기반 OAuth2 Authorization Request 저장소.
 * STATELESS 세션 정책에서 OAuth2 플로우가 정상 동작하도록 authorization request를 쿠키에 저장한다.
 */
@Component
class HttpCookieOAuth2AuthorizationRequestRepository(
    private val cookieHelper: CookieHelper,
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private val logger = LoggerFactory.getLogger(HttpCookieOAuth2AuthorizationRequestRepository::class.java)

    companion object {
        const val OAUTH2_AUTHORIZATION_REQUEST_COOKIE = "oauth2_auth_request"
        const val REDIRECT_URI_COOKIE = "redirect_uri"
        private const val COOKIE_EXPIRE_SECONDS = 180
    }

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        return cookieHelper.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE)
            ?.let { deserialize(it) }
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(response)
            return
        }

        cookieHelper.addCookie(
            response,
            OAUTH2_AUTHORIZATION_REQUEST_COOKIE,
            serialize(authorizationRequest),
            COOKIE_EXPIRE_SECONDS,
        )

        val redirectUriAfterLogin = request.getParameter(REDIRECT_URI_COOKIE)
        if (!redirectUriAfterLogin.isNullOrBlank()) {
            cookieHelper.addCookie(
                response,
                REDIRECT_URI_COOKIE,
                redirectUriAfterLogin,
                COOKIE_EXPIRE_SECONDS,
            )
        }
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? {
        return loadAuthorizationRequest(request)
    }

    fun removeAuthorizationRequestCookies(response: HttpServletResponse) {
        cookieHelper.deleteCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE)
        cookieHelper.deleteCookie(response, REDIRECT_URI_COOKIE)
    }

    private fun serialize(authorizationRequest: OAuth2AuthorizationRequest): String {
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(authorizationRequest)
            }
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray())
        }
    }

    private fun deserialize(cookie: String): OAuth2AuthorizationRequest? {
        return try {
            val bytes = Base64.getUrlDecoder().decode(cookie)
            ByteArrayInputStream(bytes).use { bais ->
                ObjectInputStream(bais).use { ois ->
                    ois.readObject() as OAuth2AuthorizationRequest
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to deserialize OAuth2 authorization request: {}", e.message)
            null
        }
    }
}
