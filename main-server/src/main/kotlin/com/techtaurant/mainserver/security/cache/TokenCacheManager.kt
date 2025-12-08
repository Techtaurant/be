package com.techtaurant.mainserver.security.cache

import com.techtaurant.mainserver.security.config.CacheType
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class TokenCacheManager(
    // RedisCacheManager 주입됨
    private val cacheManager: CacheManager
) {

    fun <T> save(cacheType: CacheType, key: String, value: T) {
        val cache = cacheManager.getCache(cacheType.cacheName)
        cache?.put(key, value)
    }

    fun <T> get(cacheType: CacheType, key: String, type: Class<T>): T? {
        val cache = cacheManager.getCache(cacheType.cacheName)
        return cache?.get(key, type)
    }

    fun delete(cacheType: CacheType, key: String) {
        val cache = cacheManager.getCache(cacheType.cacheName)
        cache?.evict(key)
    }
}
