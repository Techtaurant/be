package com.techtaurant.mainserver.user.enums

enum class UserRole(
    val key: String,
    val description: String,
) {
    USER("ROLE_USER", "USER"),
    ADMIN("ROLE_ADMIN", "ADMIN"),
    COMPANY("ROLE_COMPANY", "COMPANY"),
    ;

    companion object {
        fun fromKey(key: String): UserRole? {
            return entries.firstOrNull { it.key == key }
        }
    }
}
