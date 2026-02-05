package com.techtaurant.mainserver.security.jwt

object JwtConstants {
    const val ACCESS_TOKEN_COOKIE = "accessToken"
    const val REFRESH_TOKEN_COOKIE = "refreshToken"
    const val ACCESS_TOKEN_EXPIRED_TIME: Long = 60 * 60 * 1000L;
    const val REFRESH_TOKEN_EXPIRED_TIME: Long = 7 * 24 * 60 * 60 * 1000L;
}
