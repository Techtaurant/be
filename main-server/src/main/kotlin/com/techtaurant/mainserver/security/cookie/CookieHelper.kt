package com.techtaurant.mainserver.security.cookie

import com.techtaurant.mainserver.security.jwt.JwtConstants
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class CookieHelper(
    private val cookieProperties: CookieProperties,
) {
    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = createCookie(name, value, maxAge)
        response.addCookie(cookie)
    }

    fun deleteCookie(response: HttpServletResponse, name: String) {
        val cookie = createCookie(name, "", 0)
        response.addCookie(cookie)
    }

    fun deleteAllAuthCookies(response: HttpServletResponse) {
        deleteCookie(response, JwtConstants.REFRESH_TOKEN_COOKIE)
        deleteCookie(response, JwtConstants.ACCESS_TOKEN_COOKIE)
    }

    fun getCookie(request: HttpServletRequest, name: String): String? {
        return request.cookies?.firstOrNull { it.name == name }?.value
    }

    private fun createCookie(name: String, value: String, maxAge: Int): Cookie {
        return Cookie(name, value).apply {
            this.maxAge = maxAge
            path = cookieProperties.path
            isHttpOnly = cookieProperties.httpOnly
            secure = cookieProperties.secure
            setAttribute("SameSite", cookieProperties.sameSite)
        }
    }
}
