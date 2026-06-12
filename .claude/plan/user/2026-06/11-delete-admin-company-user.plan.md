# Delete Admin Company User

## Business Goal
관리자가 회사 사용자를 삭제할 때 회사가 만든 콘텐츠와 첨부파일, 회사 링크 데이터를 함께 정리해 남는 파일/관계 데이터가 없도록 한다.

## Scope
- **In Scope**: `DELETE /admin/companies/{companyUserId}` 추가, COMPANY 사용자 삭제 전 USER/POST attachment 삭제, 회사 작성 post/comment cascade 삭제 보장, 회사가 연결한 link 삭제 및 해당 link의 모든 `user_links` 관계 삭제, 테스트 우선 작성.
- **Out of Scope**: 일반 USER 삭제 동작 추가, 기존 post/comment/link 공개 API 동작 변경, 복잡한 커스텀/native 삭제 쿼리 추가, 운영 데이터 마이그레이션.

## Codebase Analysis Summary
`AdminCompanyController`는 회사 등록/조회/토큰 발급만 제공한다. `CompanyAdminService`는 COMPANY 검증용 `getCompanyUser`를 이미 갖고 있으며, attachment 삭제는 `AttachmentService.deleteAttachmentsByReference`가 S3와 DB를 함께 처리한다. `posts.author_id`, `comments.author_id`, `comments.post_id`, `user_links.user_id`, link 관련 로그/통계는 DB FK cascade가 설정되어 있으나 attachment는 generic reference 구조라 서비스 레이어에서 직접 삭제해야 한다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `src/test/kotlin/com/techtaurant/mainserver/user/application/CompanyAdminServiceTest.kt` | 회사 삭제 비즈니스 로직 단위 테스트 | Create |
| `src/test/kotlin/com/techtaurant/mainserver/user/infrastructure/in/AdminCompanyControllerIntegrationTest.kt` | 관리자 회사 API 통합 테스트 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/application/CompanyAdminService.kt` | 회사 관리 서비스 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/AdminCompanyController.kt` | 관리자 회사 컨트롤러 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/user/infrastructure/in/AdminCompanyControllerDocs.kt` | Swagger 문서 계약 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepository.kt` | 작성자 post 조회 | Modify |
| `src/main/kotlin/com/techtaurant/mainserver/link/infrastructure/out/UserLinkRepository.kt` | 회사 link 관계 조회/삭제 | Modify |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| Controller delete 응답 | `PostController`, `CommentController` | `@DeleteMapping` + `@ResponseStatus(HttpStatus.NO_CONTENT)`로 body 없이 응답 |
| Service transaction | 기존 service classes | 삭제 유스케이스는 `@Transactional`에서 처리 |
| Attachment cleanup | `PostWriteService.deletePost` | attachment 삭제 후 도메인 삭제 |
| Query style | 사용자 요청, repository conventions | 복잡한 custom query 대신 Spring Data JPA derived query 사용 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 회사 삭제 위치 | `CompanyAdminService.deleteCompany` | admin company 삭제에만 특수 cascade 동작을 격리 | 일반 user 삭제 서비스에 공용화 |
| post/comment 삭제 | user 삭제 FK cascade에 맡기되 post attachment는 사전 삭제 | comment attachment 모델은 없고, post attachment는 FK가 없어 직접 정리 필요 | comment/post를 모두 수동 delete |
| link 삭제 | 회사의 `UserLink`로 link를 찾고, link별 모든 `user_links` 삭제 후 link 삭제 | "해당 링크와 user links 관계를 다 삭제" 요구를 명시적으로 구현 | user_link만 삭제하고 orphan link 유지 |

## Approval Record
- User approval source: 최초 요청에 "테스트를 먼저 설계하고 그 테스트에 맞춰서 구현해줘"라고 명시됨.
- Approved scope: 관리자 회사 사용자 삭제 시 attachment/post/comment/link/user_links 정리.
- Out-of-scope items: 일반 USER 삭제, 기존 링크 저장 취소 API 동작 변경.

## Role Routing
| Todo | Owner | Dependencies | Parallelizable | Context allowed | Context forbidden |
|------|-------|--------------|----------------|-----------------|-------------------|
| Todo 1 | backend | none | no | user/post/comment/link/attachment tests and repositories | frontend |
| Todo 2 | backend | Todo 1 red | no | service/controller/docs/repositories | frontend |
| Todo 3 | qa | Todo 2 green | no | changed files and command output | unrelated refactors |

## QA Matrix
| Gate | Command/artifact | Required | Expected evidence | Owner |
|------|------------------|----------|-------------------|-------|
| RED | `./gradlew test --tests 'com.techtaurant.mainserver.user.application.CompanyAdminServiceTest' --tests 'com.techtaurant.mainserver.user.infrastructure.in.AdminCompanyControllerIntegrationTest'` | yes | new tests fail before production implementation | backend |
| Narrow GREEN | same command | yes | tests pass after implementation | backend |
| Full verification | `./gradlew test` | yes | full suite exits 0 | qa |

## External Research Log
| Question | Skill used | Source-backed conclusion | Plan impact |
|----------|------------|--------------------------|-------------|
| JPA/framework behavior | none | Local code and migrations settle required behavior | No external research |

## Cross-boundary Contracts
| Contract | Producer | Consumer | Success path | Guard/error path |
|----------|----------|----------|--------------|------------------|
| `DELETE /admin/companies/{companyUserId}` | Backend | Admin API client | `204 No Content`, company and scoped data deleted | non-COMPANY or missing user returns existing `COMPANY_NOT_FOUND` |

## Halt Conditions
- Scope drift: 일반 USER 삭제나 다른 link delete API 변경이 필요하면 중단.
- Product decision: 공유 link를 보존해야 한다는 요구가 나오면 중단.
- Security/data decision: 관리자 권한 외 삭제 허용 범위가 바뀌면 중단.
- Missing command/env: Testcontainers/PostgreSQL 또는 Gradle test 실행이 불가능하면 증거와 함께 보고.

## Implementation Todos

### Todo 1: 삭제 동작 실패 테스트 작성
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 요구된 삭제 계약을 테스트로 먼저 고정한다.
- **Work**:
  - `CompanyAdminServiceTest`를 추가해 회사 삭제 시 USER/POST attachment 정리, link별 모든 `UserLink` 삭제 후 link 삭제, company user 삭제 순서를 검증한다.
  - `AdminCompanyControllerIntegrationTest`에 `DELETE /admin/companies/{companyUserId}`가 회사 user/post/comment/attachment/link/user_links를 삭제하고 다른 사용자 데이터는 유지하는 통합 테스트를 추가한다.
- **Convention Notes**: MockK 단위 테스트는 기존 `UserWriteServiceTest` 스타일을 따른다. 통합 테스트는 기존 RestAssured 흐름과 helper 스타일을 유지한다.
- **Verification**: narrow test command가 production 구현 전 실패해야 한다.
- **Exit Criteria**: 실패가 API/서비스 메서드 부재 또는 삭제 동작 미구현 때문임을 확인한다.
- **Status**: completed

### Todo 2: admin company 삭제 구현
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: 테스트가 요구하는 삭제 동작을 JPA 기본 derived query와 서비스 로직으로 구현한다.
- **Work**:
  - `CompanyAdminService.deleteCompany(companyUserId)`를 추가한다.
  - `PostRepository.findAllByAuthorId`, `UserLinkRepository.findAllByUserId`, `UserLinkRepository.deleteAllByLink`를 추가한다.
  - `AdminCompanyController`와 docs에 `DELETE /admin/companies/{companyUserId}`를 추가하고, admin company 삭제에만 link + all user_links 삭제가 적용됨을 문서 description에 명시한다.
- **Convention Notes**: native/custom query를 추가하지 않고 Spring Data JPA derived query만 사용한다.
- **Verification**: Todo 1의 narrow test command가 통과해야 한다.
- **Exit Criteria**: 신규 테스트 전체 통과.
- **Status**: completed

### Todo 3: QA 검증 및 정리
- **Priority**: 3
- **Dependencies**: Todo 2
- **Goal**: 전체 회귀와 코드 품질을 확인한다.
- **Work**:
  - `./gradlew test`를 실행한다.
  - 필요 시 failing gate 기준으로 최소 수정 후 같은 명령을 재실행한다.
- **Convention Notes**: 테스트를 약화하거나 삭제하지 않는다.
- **Verification**: full test command exit 0.
- **Exit Criteria**: QA PASS 또는 환경 blocker 명시.
- **Status**: completed

## Verification Strategy
- RED: 신규 테스트 실패 확인.
- GREEN: 신규 단위/통합 테스트 통과 확인.
- QA: 전체 `./gradlew test` 통과 확인.

## Progress Tracking
- Total Todos: 3
- Completed: 3
- Status: Execution complete

## Change Log
- 2026-06-11: Plan created
- 2026-06-11: Todo 1 completed - failing tests added and RED compile failure confirmed
- 2026-06-11: Todo 2 completed - admin company delete behavior implemented and narrow gate passed
- 2026-06-11: Todo 3 completed - spotless and full test suite passed
