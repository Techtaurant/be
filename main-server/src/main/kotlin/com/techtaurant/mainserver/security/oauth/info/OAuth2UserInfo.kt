package com.techtaurant.mainserver.security.oauth.info

/**
 * OAuth2 Provider로부터 받은 사용자 정보를 추상화한 인터페이스
 */
interface OAuth2UserInfo {
    fun getId(): String
    fun getEmail(): String
    fun getName(): String
    fun getProfileImageUrl(): String
}
