package com.techtaurant.mainserver.user.enums

enum class UserRole(
    val key: String,
    val description: String,
) {
    USER("ROLE_USER", "USER"),
    ADMIN("ROLE_ADMIN", "ADMIN"),
}
