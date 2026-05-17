package com.techtaurant.mainserver.common.exception

import com.techtaurant.mainserver.common.status.StatusIfs

class ApiException : RuntimeException, ApiExceptionIfs {
    override val status: StatusIfs
    override val detail: String

    constructor(statusCode: StatusIfs) : super(statusCode.getDescription()) {
        this.status = statusCode
        this.detail = statusCode.getDescription()
    }

    /**
     * 세부적인 에러 메시지를 전달할 수 있는 생성자
     */
    constructor(statusCode: StatusIfs, detail: String) : super(detail) {
        this.status = statusCode
        this.detail = detail
    }

    constructor(
        statusCode: StatusIfs,
        throwable: Throwable,
    ) : super(throwable) {
        this.status = statusCode
        this.detail = statusCode.getDescription()
    }

    constructor(
        statusCode: StatusIfs,
        throwable: Throwable,
        detail: String,
    ) : super(throwable) {
        this.status = statusCode
        this.detail = detail
    }
}
