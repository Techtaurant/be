-- ShedLock 분산 락 관리 테이블
-- 여러 서버 인스턴스에서 스케줄러가 동시에 실행되지 않도록 락을 관리합니다.
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);

-- 락 만료 시간 기준 인덱스 (빠른 락 획득을 위해)
CREATE INDEX idx_shedlock_lock_until ON shedlock (lock_until);

-- 테이블 코멘트
COMMENT ON TABLE shedlock IS '분산 환경에서 스케줄러 중복 실행 방지를 위한 락 테이블';
COMMENT ON COLUMN shedlock.name IS '락 이름 (스케줄러 메서드명)';
COMMENT ON COLUMN shedlock.lock_until IS '락 만료 시간';
COMMENT ON COLUMN shedlock.locked_at IS '락 획득 시간';
COMMENT ON COLUMN shedlock.locked_by IS '락을 획득한 인스턴스 정보';
