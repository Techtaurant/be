package com.techtaurant.mainserver.security.helper

import com.techtaurant.mainserver.security.config.CookieProperties
import com.techtaurant.mainserver.security.jwt.JwtConstants
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CookieHelper(
    private val cookieProperties: CookieProperties,
) {
    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = ResponseCookie.from(name, value)
            .maxAge(Duration.ofSeconds(maxAge.toLong()))
            .path(cookieProperties.path)
            .httpOnly(cookieProperties.httpOnly)
            .secure(cookieProperties.secure)
            .sameSite(cookieProperties.sameSite)
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun deleteCookie(response: HttpServletResponse, name: String) {
        val cookie = ResponseCookie.from(name, "")
            .maxAge(Duration.ZERO)
            .path(cookieProperties.path)
            .httpOnly(cookieProperties.httpOnly)
            .secure(cookieProperties.secure)
            .sameSite(cookieProperties.sameSite)
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun deleteAllAuthCookies(response: HttpServletResponse) {
        deleteCookie(response, JwtConstants.REFRESH_TOKEN_COOKIE)
        deleteCookie(response, JwtConstants.ACCESS_TOKEN_COOKIE)
    }

    fun getCookie(request: HttpServletRequest, name: String): String? {
        return request.cookies?.firstOrNull { it.name == name }?.value
    }
}
