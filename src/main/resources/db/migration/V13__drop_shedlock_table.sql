-- ShedLock 분산 락 관리 테이블 삭제
-- 스케줄러 기능을 사용하지 않게 되어 더 이상 필요하지 않은 테이블입니다.

-- 인덱스 삭제
DROP INDEX IF EXISTS idx_shedlock_lock_until;

-- 테이블 삭제
DROP TABLE IF EXISTS shedlock;
