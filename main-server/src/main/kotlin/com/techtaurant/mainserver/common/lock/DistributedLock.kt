package com.techtaurant.mainserver.common.lock

/**
 * 분산 락 추상화 인터페이스
 * 현재는 Caffeine 기반 메모리 락을 사용하며, 추후 Redis 기반으로 확장 가능합니다.
 */
interface DistributedLock {

    /**
     * 락을 획득하고 트랜잭션 내에서 작업을 수행한 후 자동 해제합니다.
     * 트랜잭션은 락 범위 내에서 시작되어 락 해제 전에 커밋/롤백됩니다.
     *
     * @param key 락 키
     * @param ttlMillis 락 TTL (stale lock 방지), 트랜잭션 타임아웃은 이보다 짧게 설정됨
     * @param action 락 획득 후 트랜잭션 내에서 수행할 작업
     * @return 작업 결과
     * @throws ApiException 락 획득/해제 실패 또는 트랜잭션 타임아웃 시
     */
    fun <T> withLockAndTransaction(key: String, ttlMillis: Long = 30000, action: () -> T): T
}
