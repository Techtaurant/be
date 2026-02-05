package com.techtaurant.mainserver.common.lock

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.techtaurant.mainserver.common.exception.ApiException
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import kotlin.random.Random

/**
 * Caffeine 캐시 기반 메모리 락 구현체
 * 단일 인스턴스 환경에서 동시성 제어를 위해 사용합니다.
 * 트랜잭션은 락 범위 내에서 관리되어 락 만료 전 커밋/롤백이 보장됩니다.
 */
@Component
@Primary
class CaffeineLock(
    private val transactionTemplate: TransactionTemplate,
) : DistributedLock {

    companion object {
        private const val DEFAULT_TTL_MILLIS = 30000L
        private const val TRANSACTION_TIMEOUT_BUFFER_SECONDS = 3
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_DELAY_MILLIS = 50L
        private const val MAX_JITTER_MILLIS = 100L
    }

    private val lockCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(DEFAULT_TTL_MILLIS))
        .maximumSize(10000)
        .build()

    /**
     * 락을 획득하고 트랜잭션 내에서 작업을 수행한 후 자동 해제합니다.
     * 락 획득 실패 시 jitter를 적용한 재시도를 최대 3회 수행합니다.
     * 트랜잭션은 락 내부에서 시작되어 락 해제 전에 커밋/롤백됩니다.
     *
     * @param key 락 키
     * @param ttlMillis 락 TTL (트랜잭션 타임아웃은 이보다 짧게 설정됨)
     * @param action 락 획득 후 트랜잭션 내에서 수행할 작업
     * @return 작업 결과
     * @throws ApiException 락 획득 또는 해제 실패 시
     */
    override fun <T> withLockAndTransaction(key: String, ttlMillis: Long, action: () -> T): T {
        acquireLockWithRetry(key)

        try {
            val txTimeoutSeconds = calculateTransactionTimeout(ttlMillis)
            transactionTemplate.timeout = txTimeoutSeconds

            return transactionTemplate.execute { action() }
                ?: throw ApiException(LockStatus.TRANSACTION_FAILED, "Transaction returned null for key: $key")
        } finally {
            releaseLock(key)
        }
    }

    /**
     * jitter를 적용한 재시도로 락을 획득합니다.
     * 최대 MAX_RETRY_COUNT회 시도하며, 각 시도 사이에 랜덤한 대기 시간을 둡니다.
     *
     * @param key 락 키
     * @throws ApiException 모든 재시도 실패 시
     */
    private fun acquireLockWithRetry(key: String) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            val acquired = lockCache.asMap().putIfAbsent(key, true) == null
            if (acquired) return

            if (attempt < MAX_RETRY_COUNT - 1) {
                val delayMillis = BASE_DELAY_MILLIS + Random.nextLong(MAX_JITTER_MILLIS)
                Thread.sleep(delayMillis)
            }
        }
        throw ApiException(LockStatus.LOCK_ACQUISITION_FAILED, "Lock acquisition failed after $MAX_RETRY_COUNT retries for key: $key")
    }

    /**
     * 락 TTL을 기반으로 트랜잭션 타임아웃을 계산합니다.
     * 트랜잭션이 락 만료 전에 완료되도록 버퍼를 둡니다.
     */
    private fun calculateTransactionTimeout(ttlMillis: Long): Int {
        val ttlSeconds = (ttlMillis / 1000).toInt()
        return maxOf(1, ttlSeconds - TRANSACTION_TIMEOUT_BUFFER_SECONDS)
    }

    /**
     * 락을 해제합니다.
     * 락이 이미 만료되었거나 존재하지 않는 경우 트랜잭션 롤백을 위해 예외를 발생시킵니다.
     *
     * @param key 해제할 락 키
     * @throws ApiException 락 해제 실패 시 (만료로 인한 자동 해제 포함)
     */
    private fun releaseLock(key: String) {
        lockCache.asMap().remove(key) ?: throw ApiException(
            LockStatus.LOCK_RELEASE_FAILED,
            "Lock already expired or not found for key: $key"
        )
    }
}
