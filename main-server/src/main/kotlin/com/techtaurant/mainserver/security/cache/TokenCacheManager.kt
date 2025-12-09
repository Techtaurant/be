package com.techtaurant.mainserver.security.cache

import com.techtaurant.mainserver.security.config.CacheType
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

/**
 * 토큰 캐싱을 관리하는 매니저
 *
 * Spring CacheManager를 통해 캐시 추상화를 활용하여
 * 구현체(Redis, EhCache 등)에 독립적인 캐시 작업을 제공합니다.
 */
@Component
class TokenCacheManager(
    private val cacheManager: CacheManager
) {

    /**
     * 캐시에 값을 저장합니다.
     *
     * @param cacheType 캐시 타입 (TTL 및 캐시 이름 결정)
     * @param key 캐시 키
     * @param value 저장할 값
     */
    fun <T> save(cacheType: CacheType, key: String, value: T) {
        val cache = cacheManager.getCache(cacheType.cacheName)
        cache?.put(key, value)
    }

    /**
     * 캐시에서 값을 조회합니다.
     *
     * @param cacheType 캐시 타입
     * @param key 캐시 키
     * @param type 반환 타입 클래스
     * @return 캐시된 값 (없으면 null)
     */
    fun <T> get(cacheType: CacheType, key: String, type: Class<T>): T? {
        val cache = cacheManager.getCache(cacheType.cacheName)
        return cache?.get(key, type)
    }

    /**
     * 캐시에서 값을 삭제합니다.
     *
     * @param cacheType 캐시 타입
     * @param key 캐시 키
     */
    fun delete(cacheType: CacheType, key: String) {
        val cache = cacheManager.getCache(cacheType.cacheName)
        cache?.evict(key)
    }

    // ========== RefreshToken 전용 메서드 ==========

    /**
     * RefreshToken 캐시 키를 생성합니다.
     *
     * 형식: refreshToken:userId
     *
     * @param userId 사용자 ID
     * @return 캐시 키
     */

    /**
     * RefreshToken을 캐시에 저장합니다.
     *
     * @param userId 사용자 ID
     * @param token RefreshToken 값
     */
    fun saveRefreshToken(userId: String, token: String) {
        save(CacheType.REFRESH_TOKEN, userId, token)
    }

    /**
     * RefreshToken을 캐시에서 조회합니다.
     *
     * @param userId 사용자 ID
     * @return RefreshToken (없으면 null)
     */
    fun getRefreshToken(userId: String): String? {
        return get(CacheType.REFRESH_TOKEN, userId, String::class.java)
    }

    /**
     * RefreshToken을 캐시에서 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    fun deleteRefreshToken(userId: String) {
        delete(CacheType.REFRESH_TOKEN, userId)
    }
}
