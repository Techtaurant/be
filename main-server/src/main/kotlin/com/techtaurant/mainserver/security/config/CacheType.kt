package com.techtaurant.mainserver.security.config

enum class CacheType(
    val cacheName: String,
    val expiredAfterWrite: Long,
    val maximumSize: Long
) {
    ACCESS_TOKEN("accessToken", 60 * 60 * 24 * 3, 10000), // 3일
    REFRESH_TOKEN("refreshToken", 60 * 60 * 24 * 7, 10000) // 7일
}
