# 댓글 soft delete Flyway 마이그레이션 추가

## Business Goal
댓글 수정 및 삭제 기능이 배포 환경과 테스트 환경에서 동일한 스키마를 사용하도록 맞추기 위해, comments 테이블에 soft delete 컬럼을 추가하는 Flyway 마이그레이션을 적용한다.

## Scope
- **In Scope**: `comments` 테이블에 `is_deleted`, `deleted_at` 컬럼을 추가하는 `V18` 마이그레이션 작성, 기존 엔티티 변경과의 정합성 검증
- **Out of Scope**: 기존 API 로직 변경, 추가 테스트 코드 작성, 이미 생성된 커밋/PR 재정리

## Codebase Analysis Summary
현재 댓글 soft delete 구현은 `Comment` 엔티티에 `isDeleted`, `deletedAt`, `@SQLDelete`를 도입했지만, Flyway 마이그레이션에는 해당 컬럼이 아직 없어 테스트 컨텍스트가 스키마 검증에서 실패하고 있다. 기존 SQL 파일은 버전별 단일 목적 변경을 담고 있으며, PostgreSQL 기준 DDL과 인덱스를 명시적으로 관리한다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| src/main/resources/db/migration/V6__create_comment_table.sql | comments 초기 테이블 정의 | Reference |
| src/main/resources/db/migration/V16__migrate_post_pictures_to_attachments.sql | 최신 마이그레이션 스타일 참고 | Reference |
| src/main/resources/db/migration/V18__add_comment_soft_delete_columns.sql | comments soft delete 컬럼 추가 예정 | Create |
| src/main/kotlin/com/techtaurant/mainserver/comment/entity/Comment.kt | soft delete 필드를 사용하는 엔티티 | Reference |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 최소 범위 변경 | task-planner-skill / CODE_PRINCIPLES | 필요한 스키마 변경만 추가하고 다른 도메인은 건드리지 않는다 |
| SQL 버전 관리 | 기존 `db/migration` 파일들 | 버전별 단일 목적 SQL 파일을 추가하고 파일명에 변경 의도를 드러낸다 |
| Backend DB 정합성 | BACKEND.md | 엔티티와 DB 스키마 필드명을 일치시키고 검증 가능한 수준까지 확인한다 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 마이그레이션 버전 | `V18` 신규 파일 생성 | 사용자가 `V17` 배포 예정임을 명시했고 충돌 없이 후속 배포를 준비할 수 있다 | `V17` 사용 |
| 삭제 상태 기본값 | `is_deleted BOOLEAN NOT NULL DEFAULT FALSE` | 기존 댓글 데이터가 즉시 정상 상태로 간주되어야 한다 | nullable boolean |
| 삭제 시각 컬럼 | `deleted_at TIMESTAMP NULL` | 미삭제 댓글에는 값이 없어야 하며 soft delete 시점만 기록하면 된다 | NOT NULL + sentinel 값 |

## Implementation Todos

### Todo 1: 계획 파일 생성 및 범위 확정
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 요청 범위와 검증 기준을 문서화해 실행 기준을 고정한다.
- **Work**:
  - `.claude/plan/infra/2603/13-add-comment-soft-delete-migration.plan.md` 파일을 생성한다.
  - 관련 파일, 적용 컨벤션, 검증 전략을 현재 요청 범위에 맞게 정리한다.
- **Convention Notes**: 비즈니스 목표와 범위는 한국어로 명확히 작성한다.
- **Verification**: 계획 파일이 생성되고 필수 섹션이 모두 포함되어 있는지 확인한다.
- **Exit Criteria**: 실행 가능한 plan 파일이 저장되어 있다.
- **Status**: completed

### Todo 2: comments soft delete용 V18 마이그레이션 추가
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 댓글 soft delete 엔티티가 요구하는 컬럼을 Flyway로 추가한다.
- **Work**:
  - `src/main/resources/db/migration/V18__add_comment_soft_delete_columns.sql` 파일을 생성한다.
  - `comments` 테이블에 `is_deleted`, `deleted_at` 컬럼을 추가한다.
  - 기존 데이터가 안전하게 유지되도록 기본값과 nullability를 설정한다.
- **Convention Notes**: PostgreSQL DDL 문법을 사용하고 파일명은 변경 목적이 드러나야 한다.
- **Verification**: SQL 파일 내용을 검토해 엔티티 필드와 이름 및 제약조건이 일치하는지 확인한다.
- **Exit Criteria**: Flyway가 적용 가능한 단일 목적 SQL 파일이 추가되어 있다.
- **Status**: completed

### Todo 3: 정합성 검증 및 결과 정리
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 추가된 마이그레이션으로 스키마 불일치 원인을 해소했는지 확인한다.
- **Work**:
  - 관련 마이그레이션 파일과 `Comment` 엔티티 정의를 대조한다.
  - 가능한 범위에서 Gradle 테스트 또는 컴파일 검증을 수행한다.
  - 결과를 작업 노트와 최종 응답에 반영한다.
- **Convention Notes**: 실패 시 원인과 한계를 명확히 남기고, 확인하지 못한 부분은 추정으로 표현하지 않는다.
- **Verification**: `./gradlew test` 또는 대체 가능한 검증 명령 결과를 확인한다.
- **Exit Criteria**: 검증 결과와 잔여 리스크가 정리되어 있다.
- **Status**: completed

## Verification Strategy
마이그레이션 SQL과 `Comment` 엔티티를 대조한 뒤, Gradle 테스트를 실행해 Hibernate 스키마 검증 오류가 해소되는지 확인한다.
- `./gradlew test`

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-03-13: Plan created
- 2026-03-13: Todo 1 completed — 계획 파일 생성 및 범위 확정
- 2026-03-13: Todo 2 completed — comments soft delete용 V18 마이그레이션 추가
- 2026-03-13: Todo 3 completed — 정합성 검증 수행 및 결과 정리
- 2026-03-13: Execution complete
