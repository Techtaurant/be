## Not Todo

- Swagger 성공 응답 스키마를 맞추기 위해 `*ApiResponse` 같은 Swagger 전용 wrapper DTO를 새로 만들지 않는다.
- 성공 응답은 Controller/Docs 메서드의 실제 반환 타입(`ApiResponse<SomeResponse>`)에서 SpringDoc이 자동 추론하도록 둔다.
- Swagger 테스트의 media type 경로를 맞추기 위해 Controller mapping에 불필요한 `produces`를 추가하지 않는다.
- 서버 내부 시간은 항상 UTC 기준으로 동작시킨다. 별도 `Clock` Bean 또는 `TimeConfig`를 만들지 말고, JVM/JPA/Jackson 기본 시간대를 UTC로 고정한 상태에서 `Instant`, `ZoneOffset.UTC`를 명시적으로 사용한다.

## Temporal Rules

- 절대 시각(생성/수정/만료/로그 시각 등)은 PostgreSQL `TIMESTAMPTZ`, Kotlin/Java `Instant`로 관리한다.
- 날짜 단위 집계/버킷(일별 통계 키 등)은 PostgreSQL `DATE`, Kotlin/Java `LocalDate`로 관리하고 기준 날짜는 `ZoneOffset.UTC`에서 계산한다.
- `LocalDateTime`, `java.util.Date`, `java.sql.Timestamp`는 도메인/엔티티/DTO의 시간 타입으로 새로 사용하지 않는다. 외부 라이브러리 경계에서 필요할 때만 변환한다.
- 기존 `TIMESTAMP` 컬럼을 UTC 컬럼으로 전환할 때는 expand-contract 방식으로 `*_utc TIMESTAMPTZ` 컬럼을 추가하고, backfill과 old/new 컬럼 동기화 trigger를 둔 뒤 애플리케이션 read/write 경로를 UTC 컬럼으로 옮긴다.
- 운영 중 인덱스 추가는 가능한 한 `CREATE INDEX CONCURRENTLY`를 사용하고, Flyway PostgreSQL migration에서는 해당 migration이 트랜잭션 안에서 실행되지 않도록 설정을 확인한다.
