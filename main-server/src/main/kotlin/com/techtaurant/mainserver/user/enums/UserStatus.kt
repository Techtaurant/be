package com.techtaurant.mainserver.user.enums

import com.techtaurant.mainserver.common.status.StatusIfs
import org.springframework.http.HttpStatus

enum class UserStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 1002, "사용자를 찾을 수 없습니다"),
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
