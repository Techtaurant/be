package com.techtaurant.mainserver.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpireMs: Long = 3600000,
    val refreshTokenExpireMs: Long = 604800000,
)
