package com.techtaurant.mainserver.common.lock

import com.techtaurant.mainserver.common.status.StatusIfs

/**
 * 락 관련 상태 코드를 정의합니다.
 * 동시성 제어에서 발생하는 다양한 락 상태를 표현합니다.
 */
enum class LockStatus(
    private val httpStatusCode: Int,
    private val customStatusCode: Int,
    private val description: String,
) : StatusIfs {
    LOCK_RELEASE_FAILED(500, 5001, "락 해제에 실패했습니다. 트랜잭션이 롤백됩니다."),
    TRANSACTION_FAILED(500, 5002, "트랜잭션 실행에 실패했습니다."),
    LOCK_ACQUISITION_FAILED(409, 5003, "락 획득에 실패했습니다. 다른 요청이 처리 중입니다."),
    ;

    override fun getHttpStatusCode(): Int = this.httpStatusCode

    override fun getCustomStatusCode(): Int = this.customStatusCode

    override fun getDescription(): String = this.description
}
