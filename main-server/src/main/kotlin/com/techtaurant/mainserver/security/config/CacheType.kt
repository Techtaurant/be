package com.techtaurant.mainserver.security.config

import com.techtaurant.mainserver.security.jwt.JwtConstants

enum class CacheType(
    val cacheName: String,
    val expiredAfterWrite: Long,
) {
    ACCESS_TOKEN(JwtConstants.ACCESS_TOKEN_COOKIE, JwtConstants.ACCESS_TOKEN_EXPIRED_TIME),
    REFRESH_TOKEN(JwtConstants.REFRESH_TOKEN_COOKIE, JwtConstants.REFRESH_TOKEN_EXPIRED_TIME)
}
