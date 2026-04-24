# 알림 목록 조회 및 다건 읽음 처리 API 추가

## Business Goal
사용자가 자신에게 도착한 알림을 목록으로 확인하고, 여러 알림을 한 번에 읽음 처리할 수 있게 해서 알림 UX를 완성한다.

## Scope
- **In Scope**: 인증 기반 알림 목록 조회 API 추가
- **In Scope**: 인증 기반 알림 다건 읽음 처리 API 추가
- **In Scope**: DTO, 서비스, 저장소 조회/갱신 쿼리, Swagger Docs, 통합 테스트 추가
- **Out of Scope**: 실시간 푸시, 알림 삭제, unread count 전용 API, 읽지 않음 복원 API

## Codebase Analysis Summary
알림 도메인은 `Notification`, `NotificationRecipient`, `NotificationTarget` 엔티티와 알림 생성 서비스까지 구현되어 있고, 조회/읽음 처리 API 레이어는 아직 없다. 인증 API는 `@AuthenticationPrincipal userId: UUID`와 `ApiResponse`를 사용하고, 목록 API는 주로 `CursorPageResponse`를 사용한다. 컨트롤러는 `*ControllerDocs` 인터페이스를 분리하며, 검증은 RestAssured 기반 통합 테스트로 작성하는 패턴이 고정돼 있다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/main/kotlin/com/techtaurant/mainserver/notification/entity/Notification.kt` | 알림 본문/타입 엔티티 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/notification/entity/NotificationRecipient.kt` | 사용자별 수신/읽음 상태 엔티티 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/notification/entity/NotificationTarget.kt` | 알림 actor/target 매핑 엔티티 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/notification/infrastructure/out/NotificationRecipientRepository.kt` | 수신자 저장소 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/notification/infrastructure/out/NotificationRepository.kt` | 알림 저장소 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/notification/application/NotificationWriteService.kt` | 알림 생성 서비스 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/common/dto/CursorPageResponse.kt` | 커서 페이지 응답 공통 DTO | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/UserController.kt` | 인증 컨트롤러 패턴 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadLogController.kt` | 상태 변경 API 패턴 | Reference |
| `src/test/kotlin/com/techtaurant/mainserver/post/infrastructure/in/PostReadLogControllerTest.kt` | 인증 상태 변경 통합 테스트 패턴 | Reference |
| `src/test/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentReadControllerTest.kt` | 목록 조회 통합 테스트 패턴 | Reference |
| `src/main/kotlin/com/techtaurant/mainserver/notification/**` | 알림 조회/읽음 구현 대상 | Create / Modify |
| `src/test/kotlin/com/techtaurant/mainserver/notification/**` | 알림 API 테스트 대상 | Create / Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 인증 사용자 주입 | `UserController`, `PostReadLogController` | `@AuthenticationPrincipal userId: UUID`를 사용한다 |
| API 응답 래핑 | `ApiResponse`, 기존 컨트롤러들 | 성공 응답은 `ApiResponse.ok(...)` 또는 `created(...)`를 사용한다 |
| 목록 응답 | `PostController`, `CommentReadService` | 목록은 `CursorPageResponse`로 반환하고 첫 페이지는 `cursor=null`을 사용한다 |
| Swagger 문서 분리 | `*ControllerDocs` 패턴 | 컨트롤러 본체와 Docs 인터페이스를 분리한다 |
| 통합 테스트 스타일 | `PostReadLogControllerTest`, `CommentReadControllerTest` | RestAssured + 실제 JWT 토큰으로 API를 검증한다 |
| 읽음 상태 멱등성 | `PostReadLogService` | 이미 읽은 항목을 다시 읽음 처리해도 정상 처리한다 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 목록 엔드포인트 | `GET /api/notifications` | 알림 리소스의 인증 목록 조회로 가장 직관적이다 | `/api/users/me/notifications` |
| 읽음 엔드포인트 | `PATCH /api/notifications/read` | 다건 상태 변경 의미를 직접 드러낸다 | `POST /api/notifications/read-logs` |
| 페이지네이션 기준 | `notification_recipients.createdAt DESC, id DESC` | 사용자별 알림 수신 시점을 기준으로 안정적인 커서 구성이 가능하다 | `notifications.createdAt` |
| 읽음 처리 대상 검증 | 현재 사용자 소유 알림만 갱신, 나머지 ID는 무시 | 다건 API에서 멱등성과 사용성이 좋다 | 하나라도 잘못되면 4xx |
| 응답 모델 | 알림 본문 + 읽음 상태 + target 목록 포함 | 현재 스키마만으로 FE가 후속 액션에 필요한 정보를 받을 수 있다 | payload만 반환 |

## API Contracts (if applicable)

### GET `/api/notifications`
- Headers: `Authorization: Bearer <accessToken>`
- Request: `cursor: String?`, `size: Int = 20`
- Response: `ApiResponse<CursorPageResponse<NotificationListItemResponse>>`
- Note: 현재 로그인한 사용자의 수신 알림만 최신순으로 반환한다.

### PATCH `/api/notifications/read`
- Headers: `Authorization: Bearer <accessToken>`
- Request: `MarkNotificationsReadRequest { notificationIds: List<UUID> }`
- Response: `ApiResponse<Unit>`
- Note: 요청한 ID 중 현재 사용자의 미읽음 알림만 `readAt`을 현재 시각으로 갱신한다.

## Data Models (if applicable)

### NotificationRecipient
| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `UUID` | PK |
| `notification_id` | `UUID` | FK to `notifications.id` |
| `user_id` | `UUID` | FK to `users.id` |
| `read_at` | `Date?` | null이면 미읽음 |
| `created_at` | `Date` | 수신 시각, 커서 정렬 기준 |

## Implementation Todos

### Todo 1: 알림 API 통합 테스트 먼저 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 알림 목록 조회와 다건 읽음 처리의 기대 동작을 테스트로 고정한다
- **Work**:
  - `src/test/kotlin/com/techtaurant/mainserver/notification/infrastructure/in/NotificationControllerIntegrationTest.kt` 생성
  - 알림 목록 최신순 조회, 커서 조회, 읽음 상태 노출, 인증 실패, 다건 읽음 처리, 타 사용자 알림 무시 시나리오를 RestAssured로 작성
  - 필요 시 테스트 데이터 생성을 위해 기존 `NotificationWriteService` 또는 리포지토리를 사용
- **Convention Notes**: 실제 JWT 인증과 DB 상태 검증을 함께 사용한다
- **Verification**: `./gradlew test --tests "*NotificationControllerIntegrationTest"`
- **Exit Criteria**: 새 테스트가 실패하며 원하는 API 계약을 설명한다
- **Status**: completed

### Todo 2: 알림 조회/읽음 도메인 구현
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 실패 테스트를 통과시키는 최소 구현을 추가한다
- **Work**:
  - 알림 목록/커서/요청/응답 DTO 추가
  - `NotificationRecipientRepository`에 사용자별 목록 조회와 다건 읽음 갱신/조회 메서드 추가
  - `NotificationReadService` 추가하여 목록 조회와 읽음 처리 로직 구현
  - 필요한 경우 `NotificationRecipient` 엔티티에 읽음 처리 메서드 추가
- **Convention Notes**: 응답은 기존 공통 DTO와 패키지 구조를 따른다. 읽음 처리 로직은 멱등적으로 구현한다
- **Verification**: `./gradlew test --tests "*NotificationControllerIntegrationTest"`
- **Exit Criteria**: Todo 1의 테스트가 통과한다
- **Status**: completed

### Todo 3: 컨트롤러/문서/최종 검증 마무리
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 인증 API 엔드포인트와 Swagger Docs를 연결하고 최종 회귀를 확인한다
- **Work**:
  - `NotificationController`, `NotificationControllerDocs` 추가
  - `SecurityConstants.API_PREFIX` 기반 경로와 `ApiErrorResponses`를 설정
  - 알림 관련 상태 코드가 필요하면 `NotificationStatus`에 추가
  - 대상 통합 테스트와 알림 애플리케이션 테스트를 함께 실행해 회귀를 확인
- **Convention Notes**: 컨트롤러는 얇게 유지하고 서비스에 로직을 위임한다
- **Verification**: `./gradlew test --tests "*NotificationControllerIntegrationTest" --tests "*NotificationWriteServiceTest" --tests "*NotificationPayloadServiceTest"`
- **Exit Criteria**: 엔드포인트가 노출되고 관련 테스트가 통과한다
- **Status**: completed

## Verification Strategy
구현 완료 후 알림 API 전용 통합 테스트와 기존 알림 애플리케이션 테스트를 다시 실행해 회귀를 확인한다.
- `./gradlew test --tests "*NotificationControllerIntegrationTest"`
- `./gradlew test --tests "*NotificationWriteServiceTest" --tests "*NotificationPayloadServiceTest"`

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-04-24: Plan created
- 2026-04-24: Todo 1 completed — 알림 목록 조회와 다건 읽음 처리 실패 테스트 추가
- 2026-04-24: Todo 2 completed — 알림 커서/목록 DTO, 조회 서비스, 읽음 처리 로직 구현
- 2026-04-24: Todo 3 completed — 알림 컨트롤러와 Docs 추가, 관련 알림 테스트 검증 완료
- 2026-04-24: Execution complete
