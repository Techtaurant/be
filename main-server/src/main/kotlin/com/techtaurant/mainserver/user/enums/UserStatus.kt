package com.techtaurant.mainserver.user.enums

import com.techtaurant.mainserver.common.status.StatusIfs

enum class UserStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    ID_NOT_FOUND(500, 1001, "User ID not found"),
    ;

    override fun getHttpStatusCode(): Int {
        return this.httpStatusCode
    }

    override fun getCustomStatusCode(): Int {
        return this.customStatusCode
    }

    override fun getDescription(): String {
        return this.description
    }
}
