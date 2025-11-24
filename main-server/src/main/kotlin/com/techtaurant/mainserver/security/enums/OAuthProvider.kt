package com.techtaurant.mainserver.security.enums

enum class OAuthProvider(
    private val registrationId: String,
) {
    GOOGLE("google"),
    ;

    fun getRegistrationId(): String {
        return this.registrationId
    }
}
