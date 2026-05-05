package com.techtaurant.mainserver.security.enums

enum class OAuthProvider(
    private val registrationId: String,
) {
    GOOGLE("google"),
    DEV_LOCAL("dev-local"),
    SYSTEM("system"),
    ;

    fun getRegistrationId(): String {
        return this.registrationId
    }
}
