# Notification Alert Trigger Integration

## Business Goal
댓글 작성, 팔로우, 글 작성 시점에 사용자에게 필요한 알림을 누락 없이 생성해 참여 흐름을 강화하고, 기존 알림 도메인을 실제 비즈니스 쓰기 흐름에 연결한다.

## Scope
- **In Scope**: 댓글 작성 시 게시물 작성자 및 부모 체인 댓글 작성자 알림 생성, 팔로우 시 피팔로우 사용자 알림 생성, `PUBLISHED` 글 작성 시 팔로워 알림 생성, 중복 수신자 제거, 자기 자신 알림 제외, 관련 테스트 보강
- **Out of Scope**: 알림 조회/읽음 API, 비동기 큐/이벤트 도입, 푸시 전송, 기존 데이터 백필

## Codebase Analysis Summary
알림 저장 로직은 이미 `NotificationWriteService`에 존재하지만 댓글/팔로우/게시물 생성 서비스와 연결되어 있지 않다. 댓글 생성은 `CommentWriteService`, 팔로우는 `UserFollowService`, 게시물 생성은 `PostWriteService`가 각각 트랜잭션 안에서 핵심 도메인 저장을 담당하고 있어 해당 지점에 알림 생성을 연결하는 것이 현재 구조와 가장 잘 맞는다. 테스트는 서비스/통합 테스트 중심으로 작성되어 있고, 알림 저장 구조는 `Notification`, `NotificationTarget`, `NotificationRecipient` 그래프를 저장하는 방식이다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentWriteService.kt` | 댓글 생성 유스케이스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/application/PostWriteService.kt` | 게시물 생성 유스케이스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/UserFollowService.kt` | 팔로우 생성 유스케이스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/notification/application/NotificationWriteService.kt` | 알림 저장 로직 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/comment/infrastructure/out/CommentRepository.kt` | 댓글 조회/업데이트 저장소 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/out/UserFollowRepository.kt` | 팔로우 조회 저장소 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/comment/application/CommentWriteServiceTest.kt` | 댓글 생성 검증 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/post/application/PostWriteServiceTest.kt` | 게시물 생성 검증 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserControllerFollowIntegrationTest.kt` | 팔로우 통합 검증 | Modify |
| `src/test/kotlin/com/techtaurant/mainserver/notification/application/NotificationWriteServiceTest.kt` | 알림 저장 구조 검증 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 서비스 중심 트랜잭션 | `CommentWriteService`, `PostWriteService`, `UserFollowService` | 알림 생성은 기존 쓰기 서비스 트랜잭션 안에서 직접 호출한다 |
| 최소 범위 저장소 추가 | `CommentRepository`, `UserFollowRepository` | 수신자 계산에 필요한 메서드만 추가하고 조합은 서비스에서 수행한다 |
| 통합 테스트 우선 검증 | 기존 `*ServiceTest`, `*IntegrationTest` | 사용자 시나리오 기준으로 알림 엔티티 저장 결과를 검증한다 |
| HTML payload 재사용 | `NotificationPayloadService` | payload 포맷팅은 기존 알림 서비스만 사용하고 호출 서비스에서 문자열을 직접 만들지 않는다 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 알림 연결 방식 | 쓰기 서비스에서 `NotificationWriteService` 직접 호출 | 현재 코드베이스에 이벤트 버스 패턴이 없고 변경 범위가 작다 | Spring event/listener 도입 |
| 댓글 수신자 계산 | 게시물 작성자 + 부모 체인 작성자 dedupe | 요구사항을 충족하면서 중복 알림을 막는다 | 게시물 작성자만 알림, 부모 작성자만 알림 |
| 자기 자신 처리 | 수신자 목록에서 제외 | 불필요한 self notification을 막는다 | 자기 자신도 수신 |
| 글 작성 발송 조건 | `PostStatusEnum.PUBLISHED`만 알림 발송 | 사용자와 합의된 정책이다 | `PRIVATE` 포함 |
| 팔로우 알림 target | 실제 팔로우 당한 사용자로 저장 | 기존 구현의 의미 오류를 바로잡아 이후 조회 기준을 맞춘다 | actor를 target으로 유지 |

## Data Models

### Notification
| Field | Type | Constraints |
|-------|------|-------------|
| `type` | `NotificationType` | 알림 종류 |
| `payloadHtml` | `String` | 비어 있을 수 없음 |
| `targets` | `List<NotificationTarget>` | actor/target 정보 중복 없이 저장 |
| `recipients` | `List<NotificationRecipient>` | 사용자별 1회 저장 |

## Implementation Todos

### Todo 1: 알림 트리거 회귀 테스트 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 댓글/팔로우/게시물 생성 시 알림이 기대한 수신자와 target으로 저장되는지를 실패 테스트로 고정한다
- **Work**:
  - `CommentWriteServiceTest`에 일반 댓글/대댓글 작성 시 알림 생성 검증 추가
  - `PostWriteServiceTest`에 `PUBLISHED` 글 작성 시 팔로워 알림 생성과 `DRAFT` 미발송 검증 추가
  - `UserControllerFollowIntegrationTest` 또는 `UserFollowService` 관련 테스트에 팔로우 알림 생성 검증 추가
  - `NotificationWriteServiceTest`에 follow target user 저장값 검증을 실제 target 기준으로 수정
- **Convention Notes**: 기존 통합 테스트 스타일을 유지하고 DB에 저장된 `NotificationRecipient`, `NotificationTarget`를 직접 검증한다
- **Verification**: 관련 테스트만 먼저 실행해 실패를 확인한다
- **Exit Criteria**: 알림 미연결/오저장으로 인해 테스트가 명확히 실패한다
- **Status**: completed

### Todo 2: 쓰기 서비스와 알림 서비스 연결
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 각 쓰기 흐름에서 요구된 수신자에게 알림을 생성하도록 구현한다
- **Work**:
  - `CommentWriteService`에 `NotificationWriteService` 주입 및 댓글 수신자 계산 로직 추가
  - `CommentRepository`에 부모 체인 수집에 필요한 조회 메서드 추가
  - `UserFollowService`에 follow 성공 시 알림 생성 연결
  - `UserFollowRepository`에 팔로워 ID 조회 메서드 추가
  - `PostWriteService`에 `PUBLISHED` 게시물 생성 후 팔로워 알림 연결
  - `NotificationWriteService`의 follow target 저장값 수정 및 빈 recipient 처리 정리
- **Convention Notes**: 저장소는 조회만 담당하고 dedupe/self-filter/policy는 서비스에서 처리한다
- **Verification**: Todo 1에서 추가한 테스트 재실행, 필요 시 기존 notification 테스트도 실행한다
- **Exit Criteria**: 모든 신규 테스트가 통과하고 자기 자신/중복 수신자가 저장되지 않는다
- **Status**: completed

### Todo 3: 회귀 검증 및 마무리
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 변경 범위의 회귀를 확인하고 plan 상태를 완료로 정리한다
- **Work**:
  - 변경된 도메인의 서비스/통합 테스트 묶음 실행
  - 실패/불안정 케이스가 있으면 최소 수정으로 정리
  - plan 파일의 진행 상태와 change log 갱신
- **Convention Notes**: 완료 주장은 실제 실행 결과로만 한다
- **Verification**: 지정 테스트 스위트 실행
- **Exit Criteria**: 관련 테스트가 통과하고 plan 진행 상태가 최신 상태로 반영된다
- **Status**: completed

## Verification Strategy
관련 서비스/통합 테스트를 단계적으로 실행해 red-green을 확인하고, 마지막에 변경 범위를 다시 한 번 묶어서 검증한다.
- `./gradlew test --tests 'com.techtaurant.mainserver.notification.application.NotificationWriteServiceTest'`
- `./gradlew test --tests 'com.techtaurant.mainserver.comment.application.CommentWriteServiceTest'`
- `./gradlew test --tests 'com.techtaurant.mainserver.post.application.PostWriteServiceTest'`
- `./gradlew test --tests 'com.techtaurant.mainserver.user.infrastructure.in.UserControllerFollowIntegrationTest'`

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-04-24: Plan created
- 2026-04-24: Todo 1 completed — 댓글/팔로우/게시물 알림 트리거 회귀 테스트 추가 및 실패 확인
- 2026-04-24: Todo 2 completed — 댓글/팔로우/게시물 쓰기 서비스에 알림 생성 연결 및 FOLLOW target 저장 수정
- 2026-04-24: Todo 3 completed — 관련 서비스/통합 테스트 재실행 및 기존 첨부파일 테스트 회귀 확인
