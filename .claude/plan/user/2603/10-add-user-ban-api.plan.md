# 사용자 ban API 및 조회 차단 정책 구현

## Business Goal
특정 사용자가 원치 않는 다른 사용자를 개인적으로 차단할 수 있게 하고, 차단한 사용자의 게시물은 조회되지 않도록 하며 댓글은 맥락은 유지하되 식별 정보가 노출되지 않도록 처리한다.

## Scope
- **In Scope**: 사용자 ban/ban 목록/unban API, `user_bans` 저장 구조, 게시물 목록/상세 차단 필터, 댓글 마스킹 및 `isBanned` 응답 필드, 관련 Swagger 및 테스트 추가
- **Out of Scope**: 관리자 차단 기능, 알림 발송, 기존 데이터 백필 외 별도 운영 도구, 댓글 작성 제한 같은 추가 정책

## Codebase Analysis Summary
사용자 API는 `user.infrastructure.in` 패키지의 controller/docs와 `user.application` 서비스 조합으로 구성되어 있다. 게시물 목록은 `PostListReadService`와 `PostRepositoryCustomImpl`이 조건별 조회를 담당하며, 게시물 상세는 `PostDetailReadService`에서 접근 제어를 수행한다. 댓글 조회는 `CommentReadService`가 응답 DTO 변환 직전에 현재 사용자별 상태를 붙이는 구조다. 따라서 게시물 차단은 repository/service 레벨에서 필터링하고, 댓글 마스킹은 service의 DTO 매핑 단계에 추가하는 방식이 현재 구조와 가장 잘 맞는다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/resources/db/migration` | DB 마이그레이션 저장소 | Create |
| `src/main/kotlin/com/techtaurant/mainserver/user/entity/User.kt` | 사용자 엔티티 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/out/UserRepository.kt` | 사용자 조회 리포지토리 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/UserReadService.kt` | 사용자 조회 서비스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserController.kt` | 사용자 인증 API | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserControllerDocs.kt` | 사용자 Swagger 계약 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostListReadService.kt` | 게시물 목록 조회 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostDetailReadService.kt` | 게시물 상세 조회 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepositoryCustom.kt` | 게시물 조건 조회 인터페이스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepositoryCustomImpl.kt` | 게시물 조건 조회 구현 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentReadService.kt` | 댓글 조회 서비스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/dto/CommentListResponse.kt` | 댓글 응답 DTO | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/post/application/PostListReadServiceTest.kt` | 게시물 목록 단위 테스트 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentReadControllerTest.kt` | 댓글 조회 통합 테스트 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| DTO 네이밍과 설명 | `.claude/core/BACKEND.md` | Request/Response 접미사 유지, Swagger description은 한국어 작성 |
| 코드 단순성 | `.claude/core/CODE_PRINCIPLES.md` | 작은 함수로 분리하고 과도한 추상화 없이 현재 요구만 구현 |
| 테스트 패턴 | `.claude/framework/SPRING_BOOT.md` | 통합 테스트는 기존 `IntegrationTest` 기반, Given-When-Then 구조 유지 |
| 상태 코드 관리 | 기존 `UserStatus` enum | 새로운 예외는 enum에 추가하고 controller docs에도 명시 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| Ban 저장 방식 | `user_bans` 별도 테이블 | 사용자 간 N:M 관계를 명확히 표현하고 조회 정책을 독립적으로 확장하기 쉽다 | `users` self relation |
| 게시물 상세 차단 동작 | `POST_NOT_FOUND`로 숨김 | 기존 비공개 게시물 접근 차단과 같은 UX를 유지한다 | 상세는 노출하되 마스킹 |
| 댓글 차단 동작 | 댓글 유지 + `isBanned=true` + `banned_<6글자>` 치환 | 스레드 맥락은 유지하면서 식별 정보는 제거할 수 있다 | 댓글 완전 제외 |
| 해시 규칙 | 결정적 SHA-256 기반 6글자 prefix | 동일 작성자의 댓글이 같은 익명 식별자로 보여 추적 가능성이 낮고 읽기 맥락이 유지된다 | 랜덤 익명값, 고정 문구 |

## API Contracts

### `POST /api/users/{targetUserId}/ban`
- Headers: 인증 필요
- Request: 없음
- Response: `ApiResponse<UserBanResponse>`
- Note: 자기 자신 차단 및 중복 차단은 예외 처리

### `GET /api/users/me/bans`
- Headers: 인증 필요
- Request: 없음
- Response: `ApiResponse<List<UserBanListItemResponse>>`
- Note: 현재 사용자가 차단한 사용자 목록을 최신순으로 반환

### `DELETE /api/users/{targetUserId}/ban`
- Headers: 인증 필요
- Request: 없음
- Response: `204 No Content`
- Note: 존재하지 않는 차단 관계는 예외 처리

## Data Models

### UserBan
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK |
| user_id | UUID | FK users.id, not null |
| banned_user_id | UUID | FK users.id, not null |
| created_at | timestamp | UTC, not null |
| updated_at | timestamp | UTC, not null |

## Implementation Todos

### Todo 1: ban 저장 구조와 도메인 모델 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 사용자 차단 관계를 저장하고 조회할 수 있는 최소 도메인 구조를 만든다.
- **Work**:
  - `V17__create_user_bans.sql` 마이그레이션 추가
  - `userban` 도메인 패키지 또는 기존 user 하위에 entity/repository 추가
  - 중복 차단 방지를 위한 unique constraint 및 조회 메서드 구현
  - `UserStatus`에 ban 관련 예외 코드 추가
- **Convention Notes**: 명확한 이름을 사용하고 enum/엔티티는 별도 파일로 분리한다.
- **Verification**: 관련 코드 컴파일 확인, repository 사용 경로 점검
- **Exit Criteria**: user ban 엔티티와 repository가 생성되고 예외 상태가 정의된다.
- **Status**: completed

### Todo 2: 사용자 ban API와 목록 조회 API 구현
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 사용자가 다른 사용자를 차단/해제/목록 조회할 수 있는 인증 API를 제공한다.
- **Work**:
  - ban 응답 DTO와 목록 DTO 추가
  - `UserBanService` 또는 역할이 명확한 서비스 추가
  - `UserController`/`UserControllerDocs`에 POST, GET, DELETE 엔드포인트 추가
  - 자기 자신 차단, 대상 사용자 없음, 중복 차단, 차단 관계 없음 검증 추가
- **Convention Notes**: Swagger description은 한국어로 작성하고 `ApiErrorResponses`/`ApiErrorCodeResponses` 패턴을 따른다.
- **Verification**: 사용자 API 관련 테스트 추가 또는 실행
- **Exit Criteria**: 세 API가 동작하고 예외가 문서화된다.
- **Status**: completed

### Todo 3: 게시물 조회 차단 정책 반영
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 로그인 사용자가 차단한 작성자의 게시물은 목록과 상세에서 보이지 않게 한다.
- **Work**:
  - 현재 사용자 기준 ban 대상 ID 조회 로직 추가
  - `PostRepositoryCustom`/`PostRepositoryCustomImpl`에 제외 author 조건 확장
  - `PostListReadService`에서 ban 대상 작성자 제외 적용
  - `PostDetailReadService`에서 차단된 작성자 게시물 상세 접근 시 `POST_NOT_FOUND` 처리
- **Convention Notes**: 게시물 접근 제어는 service/repository 레이어에서 일관되게 처리한다.
- **Verification**: `PostListReadServiceTest`와 상세 조회 테스트 실행
- **Exit Criteria**: 로그인 사용자는 차단 대상 작성자의 게시물을 목록/상세에서 볼 수 없다.
- **Status**: completed

### Todo 4: 댓글 마스킹 정책 반영
- **Priority**: 3
- **Dependencies**: Todo 1
- **Goal**: 차단한 작성자의 댓글은 조회는 유지하되 익명화된 응답으로 반환한다.
- **Work**:
  - `CommentListResponse`에 `isBanned` 필드 추가
  - `CommentReadService`에서 현재 사용자 ban 목록을 로드해 댓글 응답 매핑 시 마스킹 적용
  - `banned_<6글자>` 형식의 결정적 해시 유틸 추가
  - 댓글 내용, 작성자명, 프로필 이미지 URL을 마스킹 규칙으로 치환
- **Convention Notes**: 마스킹 로직은 재사용 가능한 작은 함수로 분리하고 DTO 변환 책임을 흐리지 않도록 한다.
- **Verification**: 댓글 조회 통합 테스트 또는 서비스 테스트 실행
- **Exit Criteria**: 차단된 사용자의 댓글은 `isBanned=true`이며 식별 정보가 노출되지 않는다.
- **Status**: completed

### Todo 5: 회귀 테스트와 문서 정리
- **Priority**: 4
- **Dependencies**: Todo 2, Todo 3, Todo 4
- **Goal**: 기능이 기존 동작을 깨지 않았는지 검증하고 계획 상태를 마무리한다.
- **Work**:
  - 변경된 단위/통합 테스트 실행
  - 필요한 경우 API 문서 설명 보정
  - 계획 파일 진행률과 변경 로그 갱신
- **Convention Notes**: 실패한 테스트를 남기지 않고, 테스트 범위는 변경 영향 파일에 집중한다.
- **Verification**: `./gradlew test --tests ...` 또는 관련 테스트 묶음 실행
- **Exit Criteria**: 관련 테스트가 통과하고 계획 파일이 완료 상태로 정리된다.
- **Status**: completed

## Verification Strategy
변경 영향이 큰 영역은 사용자 API, 게시물 읽기, 댓글 읽기다. 따라서 ban 서비스/컨트롤러 테스트, 게시물 목록/상세 테스트, 댓글 조회 테스트를 우선 실행하고 마지막에 관련 테스트 묶음 또는 전체 테스트를 가능한 범위에서 재실행한다.
- `./gradlew test --tests "*User*Ban*" --tests "*PostListReadServiceTest*" --tests "*CommentReadControllerTest*"`
- 필요 시 `./gradlew test --tests "*Post*"` 처럼 범위를 넓혀 회귀 확인

## Progress Tracking
- Total Todos: 5
- Completed: 5
- Status: Execution complete

## Change Log
- 2026-03-10: Plan created
- 2026-03-10: Todo 1 completed — `user_bans` 마이그레이션, 엔티티, 리포지토리, 예외 상태 추가
- 2026-03-10: Todo 2 completed — 사용자 ban, ban 목록 조회, unban API와 DTO/서비스 추가
- 2026-03-10: Todo 3 completed — 게시물 목록/상세에서 차단한 작성자 비노출 처리
- 2026-03-10: Todo 4 completed — 댓글 `isBanned` 및 `banned_<6글자>` 마스킹 정책 반영
- 2026-03-10: Todo 5 completed — JDK 17로 타깃 테스트 실행 완료, JaCoCo 전역 커버리지 게이트는 별도 관리 이슈로 확인
