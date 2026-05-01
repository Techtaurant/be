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
    CANNOT_BAN_SELF(HttpStatus.BAD_REQUEST.value(), 1003, "자기 자신은 차단할 수 없습니다"),
    USER_ALREADY_BANNED(HttpStatus.CONFLICT.value(), 1004, "이미 차단한 사용자입니다"),
    USER_BAN_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 1005, "차단한 사용자 관계를 찾을 수 없습니다"),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST.value(), 1006, "자기 자신은 팔로우할 수 없습니다"),
    USER_ALREADY_FOLLOWED(HttpStatus.CONFLICT.value(), 1007, "이미 팔로우한 사용자입니다"),
    USER_FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 1008, "팔로우 관계를 찾을 수 없습니다"),
    USER_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT.value(), 1009, "이미 사용 중인 닉네임입니다"),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND.value(), 1010, "회사를 찾을 수 없습니다"),
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
