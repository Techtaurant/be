package com.techtaurant.mainserver.common.status

import org.springframework.http.HttpStatus

enum class DefaultStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    OK(200, 200, "OK"),
    BAD_REQUEST(400, 400, "Wrong Request"),
    NOT_FOUND(404, 404, "Not Found"),
    SERVER_ERROR(500, 500, "Server Error"),

    // 제일 마지막에
    UNDEFINED_STATUS(HttpStatus.INTERNAL_SERVER_ERROR.value(), 999, "Undefined status occurred"),
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
