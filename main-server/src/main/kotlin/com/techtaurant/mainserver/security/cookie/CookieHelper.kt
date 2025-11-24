package com.techtaurant.mainserver.security.cookie

import jakarta.servlet.http.Cookie
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
