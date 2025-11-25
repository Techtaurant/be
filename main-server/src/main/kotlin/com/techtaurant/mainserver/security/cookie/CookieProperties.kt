package com.techtaurant.mainserver.security.cookie

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cookie")
data class CookieProperties(
    val secure: Boolean = true,
    val httpOnly: Boolean = true,
    val sameSite: String = "Lax",
    val path: String = "/",
)
