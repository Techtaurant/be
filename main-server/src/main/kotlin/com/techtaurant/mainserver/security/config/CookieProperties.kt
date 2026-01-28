package com.techtaurant.mainserver.security.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cookie")
data class CookieProperties(
    val secure: Boolean = false,
    val httpOnly: Boolean = true,
    val sameSite: String = "None",
    val path: String = "/",
)
