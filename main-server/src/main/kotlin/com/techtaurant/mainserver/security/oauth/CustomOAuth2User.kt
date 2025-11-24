package com.techtaurant.mainserver.security.oauth

import com.techtaurant.mainserver.user.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class CustomOAuth2User(
    private val user: User,
    private val attributes: Map<String, Any>,
) : OAuth2User {

    override fun getName(): String = user.name

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority(user.role.key))

    fun getUser(): User = user
}
