# Event Sourcing -> Atomic Update 전환

## Business Goal
게시물 통계(viewCount, likeCount, commentCount)를 스케줄러 기반 배치 동기화 방식에서
이벤트 발생 시점의 DB 레벨 원자적 UPDATE 방식으로 전환하여 실시간 정합성과 성능을 개선한다.
PostDailyStats는 유지하되, 스케줄러 없이 이벤트 발생 시 원자적으로 증분 기록한다.

## Scope
- **In Scope**:
  - PostRepository에 원자적 increment/decrement 쿼리 추가
  - PostDailyStatsRepository에 원자적 increment 쿼리 추가
  - PostViewLogService에서 조회 시 viewCount 즉시 증분
  - PostLikeLogService에서 좋아요 토글 시 likeCount 증분/감소
  - CommentWriteService에서 댓글 생성 시 commentCount 즉시 증분
  - PostDailyStats 첫 insert 시 Unique Constraint 예외 처리로 동시성 해결
  - PostStatsScheduler.kt 삭제
  - PostStatsService.kt 삭제
  - Post 엔티티에서 statsUpdatedAt 필드 제거
  - PostViewLogRepository, PostLikeLogRepository에서 스케줄러 전용 메서드 제거
- **Out of Scope**:
  - ShedLock 설정 제거 (다른 스케줄러가 추가될 수 있음)
  - 테스트 코드 수정
  - PostDailyStats 엔티티 구조 변경 (이미 unique constraint 존재)

## Codebase Analysis Summary
현재 구조에서 PostStatsScheduler가 60초마다 view_log, like_log, comment 테이블에서
이벤트가 발생한 게시물을 찾아 count를 재계산하여 Post 엔티티에 반영한다.
PostStatsService는 일일 통계를 PostDailyStats에 저장한다.
이 두 클래스를 삭제하고, 각 서비스(ViewLog, LikeLog, Comment)에서
이벤트 발생 시점에 원자적으로 Post.count와 PostDailyStats를 갱신한다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `post/infrastructure/out/PostRepository.kt` | Post JPA Repository | Modify - 원자적 increment/decrement 쿼리 추가 |
| `post/infrastructure/out/PostDailyStatsRepository.kt` | DailyStats JPA Repository | Modify - 원자적 increment 쿼리 추가 |
| `post/application/PostViewLogService.kt` | 조회 로그 서비스 | Modify - viewCount 원자적 증분 호출 추가 |
| `post/application/PostLikeLogService.kt` | 좋아요 로그 서비스 | Modify - likeCount 원자적 증분/감소 호출 추가 |
| `comment/application/CommentWriteService.kt` | 댓글 작성 서비스 | Modify - commentCount 원자적 증분 호출 추가 |
| `post/application/PostStatsScheduler.kt` | 통계 스케줄러 | Delete |
| `post/application/PostStatsService.kt` | 통계 서비스 | Delete |
| `post/entity/Post.kt` | Post 엔티티 | Modify - statsUpdatedAt 제거, KDoc 수정 |
| `post/infrastructure/out/PostViewLogRepository.kt` | ViewLog Repository | Modify - 스케줄러 전용 메서드 제거 |
| `post/infrastructure/out/PostLikeLogRepository.kt` | LikeLog Repository | Modify - 스케줄러 전용 메서드 제거 |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| KDoc 한국어 주석 | CODE_PRINCIPLES.md | 모든 public 메서드에 KDoc 작성, @param/@return 포함 |
| @Modifying 어노테이션 | 기존 코드 패턴 (사용자 제시 예시) | UPDATE 쿼리에 clearAutomatically=true, flushAutomatically=true |
| 네이밍 | CODE_PRINCIPLES.md | increment/decrement + 대상 필드명 |
| Transactional | 기존 서비스 패턴 | 서비스 메서드에 @Transactional |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 통계 갱신 방식 | 원자적 DB UPDATE | Race condition 방지, 실시간 반영 | 스케줄러 배치, Redis 캐시 |
| DailyStats 동시성 | DB Unique Constraint + 예외 처리 | 이미 unique constraint 존재, 추가 인프라 불필요 | UPSERT, 분산 락 |
| 좋아요 토글 판별 | 이전 isLiked 값 비교 | 트랜잭션 내에서 안전, 동일 사용자 동시 토글은 현실적으로 희박 | 매번 count 재계산 |

## Data Models

### PostDailyStats (기존, 변경 없음)
| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK |
| post_id | UUID | FK, NOT NULL, UNIQUE(post_id, stat_date) |
| stat_date | LocalDate | NOT NULL, UNIQUE(post_id, stat_date) |
| view_count | Long | NOT NULL, default 0 |
| like_count | Long | NOT NULL, default 0 |
| comment_count | Long | NOT NULL, default 0 |

## Implementation Todos

### Todo 1: PostRepository에 원자적 increment/decrement 쿼리 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Post의 viewCount, likeCount, commentCount를 DB 레벨에서 원자적으로 증감하는 메서드 추가
- **Work**:
  - `PostRepository.kt`에 아래 메서드 추가:
    - `incrementViewCount(postId: UUID)` - `UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId`
    - `incrementLikeCount(postId: UUID)` - `UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId`
    - `decrementLikeCount(postId: UUID)` - `UPDATE Post p SET p.likeCount = GREATEST(p.likeCount - 1, 0) WHERE p.id = :postId`
    - `incrementCommentCount(postId: UUID)` - `UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId`
  - 모든 메서드에 `@Modifying(clearAutomatically = true, flushAutomatically = true)` 적용
  - KDoc 주석 작성
- **Convention Notes**: 기존 PostRepository 패턴 준수, @Query + @Param 사용
- **Verification**: 빌드 성공
- **Exit Criteria**: 4개 메서드가 PostRepository에 추가되고 컴파일 성공
- **Status**: pending

### Todo 2: PostDailyStatsRepository에 원자적 increment 쿼리 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: PostDailyStats의 각 count를 원자적으로 증분하는 메서드 추가
- **Work**:
  - `PostDailyStatsRepository.kt`에 아래 메서드 추가:
    - `incrementViewCount(postId: UUID, statDate: LocalDate)` - native query로 `UPDATE post_daily_stats SET view_count = view_count + 1, updated_at = NOW() WHERE post_id = :postId AND stat_date = :statDate`
    - `incrementLikeCount(postId: UUID, statDate: LocalDate)`
    - `decrementLikeCount(postId: UUID, statDate: LocalDate)` - `GREATEST(like_count - 1, 0)` 사용
    - `incrementCommentCount(postId: UUID, statDate: LocalDate)`
  - 모든 메서드에 `@Modifying(clearAutomatically = true, flushAutomatically = true)` 적용
  - 기존 `findByPostIdAndStatDate`는 유지 (DailyStats 생성 시 사용)
  - KDoc 주석 작성
- **Convention Notes**: native query 사용 (PostDailyStats는 JPQL에서 테이블명 매핑 이슈)
- **Verification**: 빌드 성공
- **Exit Criteria**: 4개 increment/decrement 메서드가 추가되고 컴파일 성공
- **Status**: pending

### Todo 3: PostDailyStats 증분 헬퍼 서비스 생성
- **Priority**: 2
- **Dependencies**: Todo 2
- **Goal**: DailyStats 레코드가 없으면 생성 + 증분, 있으면 증분만 수행하는 로직 (Unique Constraint 예외 처리 포함)
- **Work**:
  - `post/application/PostDailyStatsService.kt` 생성
  - 메서드: `incrementDailyStat(postId: UUID, field: DailyStatField)` where `DailyStatField`은 enum(VIEW, LIKE, COMMENT)
  - 메서드: `decrementDailyStat(postId: UUID, field: DailyStatField)`
  - 로직:
    1. `postDailyStatsRepository.incrementXxxCount(postId, today)` 호출
    2. 영향 받은 row가 0이면 (레코드 미존재) → 새 PostDailyStats 생성 시도
    3. `DataIntegrityViolationException` 발생 시 (동시 insert) → 다시 increment 호출
  - 의존성: `PostDailyStatsRepository`, `PostRepository`
  - `@Transactional(propagation = Propagation.REQUIRES_NEW)` 사용하여 외부 트랜잭션과 분리
- **Convention Notes**: @Service 어노테이션, KDoc 주석, 한국어 로그
- **Verification**: 빌드 성공
- **Exit Criteria**: PostDailyStatsService가 생성되고 컴파일 성공
- **Status**: pending

### Todo 4: PostViewLogService에 원자적 viewCount 증분 추가
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 3
- **Goal**: 조회 로그 기록 시 Post.viewCount와 PostDailyStats.viewCount를 즉시 원자적으로 증분
- **Work**:
  - `PostViewLogService.kt` 수정
  - 생성자에 `PostRepository`, `PostDailyStatsService` 의존성 추가 (PostRepository는 이미 있음)
  - `recordView()` 메서드 끝에 추가:
    - `postRepository.incrementViewCount(postId)`
    - `postDailyStatsService.incrementDailyStat(postId, DailyStatField.VIEW)`
- **Convention Notes**: 기존 메서드 시그니처 유지
- **Verification**: 빌드 성공
- **Exit Criteria**: recordView 호출 시 viewCount가 원자적으로 증분되는 코드 추가
- **Status**: pending

### Todo 5: PostLikeLogService에 원자적 likeCount 증분/감소 추가
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 3
- **Goal**: 좋아요 토글 시 이전 값과 비교하여 Post.likeCount와 PostDailyStats.likeCount를 원자적으로 증감
- **Work**:
  - `PostLikeLogService.kt` 수정
  - 생성자에 `PostDailyStatsService` 의존성 추가
  - `recordLike()` 메서드 수정:
    - 기존 로그가 없고 isLiked=true → `postRepository.incrementLikeCount(postId)` + dailyStats increment
    - 기존 로그 있고 이전값!=새값:
      - false→true → `postRepository.incrementLikeCount(postId)` + dailyStats increment
      - true→false → `postRepository.decrementLikeCount(postId)` + dailyStats decrement
    - 기존 로그 있고 이전값==새값 → count 변경 없음
- **Convention Notes**: 기존 메서드 시그니처 유지, 이전 isLiked 값을 변수에 저장 후 비교
- **Verification**: 빌드 성공
- **Exit Criteria**: 좋아요 토글에 따라 likeCount가 정확히 증감되는 로직 구현
- **Status**: pending

### Todo 6: CommentWriteService에 원자적 commentCount 증분 추가
- **Priority**: 2
- **Dependencies**: Todo 1, Todo 3
- **Goal**: 댓글 생성 시 Post.commentCount와 PostDailyStats.commentCount를 즉시 원자적으로 증분
- **Work**:
  - `CommentWriteService.kt` 수정
  - 생성자에 `PostRepository` (post 패키지), `PostDailyStatsService` 의존성 추가 (PostRepository는 이미 있음)
  - `createComment()` 메서드에서 `commentRepository.save(comment)` 이후 추가:
    - `postRepository.incrementCommentCount(request.postId)`
    - `postDailyStatsService.incrementDailyStat(request.postId, DailyStatField.COMMENT)`
- **Convention Notes**: 기존 메서드 시그니처 유지
- **Verification**: 빌드 성공
- **Exit Criteria**: createComment 호출 시 commentCount가 원자적으로 증분되는 코드 추가
- **Status**: pending

### Todo 7: Post 엔티티에서 statsUpdatedAt 제거
- **Priority**: 1
- **Dependencies**: none
- **Goal**: 더 이상 사용되지 않는 statsUpdatedAt 필드를 Post 엔티티에서 제거
- **Work**:
  - `Post.kt`에서 `statsUpdatedAt` 프로퍼티 제거
  - KDoc에서 `@property statsUpdatedAt` 라인 제거
  - KDoc에서 viewCount, likeCount, commentCount 설명을 "배치로 동기화" -> "원자적 증분" 으로 수정
- **Convention Notes**: 엔티티 변경이므로 DDL 영향 확인
- **Verification**: 빌드 성공
- **Exit Criteria**: statsUpdatedAt 필드가 Post 엔티티에서 완전히 제거
- **Status**: pending

### Todo 8: PostStatsScheduler.kt 및 PostStatsService.kt 삭제
- **Priority**: 2
- **Dependencies**: Todo 4, Todo 5, Todo 6, Todo 7
- **Goal**: 더 이상 필요 없는 스케줄러와 배치 통계 서비스 삭제
- **Work**:
  - `post/application/PostStatsScheduler.kt` 파일 삭제
  - `post/application/PostStatsService.kt` 파일 삭제
- **Convention Notes**: 삭제 전 다른 곳에서 참조하지 않는지 확인 (분석 결과 참조 없음)
- **Verification**: 빌드 성공
- **Exit Criteria**: 두 파일이 삭제되고 빌드 오류 없음
- **Status**: pending

### Todo 9: Repository에서 스케줄러 전용 메서드 제거
- **Priority**: 2
- **Dependencies**: Todo 8
- **Goal**: 스케줄러에서만 사용하던 Repository 메서드 제거
- **Work**:
  - `PostViewLogRepository.kt`에서 `findDistinctPostIdsByCreatedAtAfter` 메서드 제거
  - `PostLikeLogRepository.kt`에서 `findDistinctPostIdsByUpdatedAtAfter` 메서드 제거
  - `PostViewLogRepository.kt`에서 `countByPostId` 메서드 제거 (스케줄러에서만 사용)
  - `PostLikeLogRepository.kt`에서 `countByPostIdAndIsLikedTrue` 메서드 제거 (스케줄러에서만 사용)
  - `CommentRepository.kt`에서 `countByPostId` 메서드 제거 (스케줄러에서만 사용)
  - 각 메서드가 다른 곳에서 참조되지 않는지 최종 확인 후 제거
- **Convention Notes**: 사용되지 않는 코드는 완전 삭제
- **Verification**: 빌드 성공
- **Exit Criteria**: 스케줄러 전용 메서드가 모두 제거되고 빌드 오류 없음
- **Status**: pending

### Todo 10: 최종 빌드 검증
- **Priority**: 3
- **Dependencies**: Todo 1~9
- **Goal**: 전체 프로젝트 빌드 및 컴파일 성공 확인
- **Work**:
  - `./gradlew :main-server:compileKotlin` 실행
  - 컴파일 오류 확인 및 수정
- **Convention Notes**: -
- **Verification**: 빌드 성공
- **Exit Criteria**: 컴파일 오류 0개
- **Status**: pending

## Verification Strategy
- `./gradlew :main-server:compileKotlin` 빌드 성공
- 삭제된 파일에 대한 참조가 남아있지 않은지 grep 검증
- 원자적 쿼리가 올바른 JPQL/native SQL 문법인지 확인

## Progress Tracking
- Total Todos: 10
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-07: Plan created
