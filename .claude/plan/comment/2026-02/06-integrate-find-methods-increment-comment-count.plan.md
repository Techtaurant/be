# CommentWriteService 리팩토링 및 댓글 수 원자적 증가 구현

## Business Goal
댓글이 작성될 때마다 Post 엔티티의 commentCount를 실시간으로 원자적으로 증가시켜 통계 정확성을 향상시키고, CommentWriteService의 중복 헬퍼 메서드를 통합하여 코드 가독성을 개선합니다.

## Scope
- **In Scope**:
  - PostRepository에 `incrementCommentCount` 메서드 추가 (@Modifying + JPQL UPDATE)
  - CommentWriteService의 `findPostById`와 `findUserById` 통합 → `validatePostAndAuthor`
  - `createComment` 메서드에서 댓글 저장 후 commentCount 원자적 증가
  - 기존 통합 테스트 수정 (commentCount 증가 검증 추가)

- **Out of Scope**:
  - 댓글 삭제 시 commentCount 감소 (별도 작업)
  - 기존 배치 통계 로직 수정 (PostStatsService는 그대로 유지)
  - 대대댓글 작성 시 별도 처리 (현재와 동일)

## Codebase Analysis Summary

현재 CommentWriteService는 다음과 같은 구조를 가지고 있습니다:
- `createComment`: 댓글 생성 로직 (@Transactional)
- `findUserById`: User 존재 확인 (orElseThrow)
- `findPostById`: Post 존재 확인 (orElseThrow)
- `resolveParent`: 부모 댓글 검증 로직

Post 엔티티는 `commentCount` 필드를 가지고 있으나, 현재는 배치 작업(PostStatsService)에서만 업데이트됩니다.

PostRepository는 JpaRepository를 상속받고 있으며, 커스텀 JPQL 쿼리를 사용하는 여러 메서드들이 있습니다.

### Relevant Files
| File | Role | Action |
|------|------|--------|
| `main-server/src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepository.kt` | Post 엔티티 Repository | Modify - incrementCommentCount 메서드 추가 |
| `main-server/src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentWriteService.kt` | 댓글 작성 서비스 | Modify - 메서드 통합 및 카운트 증가 로직 추가 |
| `main-server/src/test/kotlin/com/techtaurant/mainserver/comment/application/CommentWriteServiceTest.kt` | CommentWriteService 테스트 | Modify - commentCount 검증 추가 |

### Conventions to Follow
| Convention | Source | Rule |
|-----------|--------|------|
| 메서드 네이밍 | CODE_PRINCIPLES.md | `validate*` (검증), `increment*` (증가) - 동작 명확화 |
| 주석 스타일 | CODE_PRINCIPLES.md | 한국어, 파라미터/반환값 명시, 예외 케이스 문서화 |
| SOLID 원칙 | BACKEND.md | Single Responsibility - 검증과 증가 로직 분리 |
| JPQL 사용 | PostRepository 기존 패턴 | @Query + JPQL로 커스텀 쿼리 작성 |
| 테스트 패턴 | SPRING_BOOT.md | Given-When-Then, @DisplayName 한글, AssertJ 사용 |

## Architecture Decisions
| Decision | Choice | Rationale | Alternatives |
|----------|--------|-----------|--------------|
| 증가 방식 | @Modifying + JPQL UPDATE | 락 없이 DB 레벨 원자성 보장, 동시성 안전 | Native Query (DB 종속적), Pessimistic Lock (성능 저하) |
| 메서드 통합 | validatePostAndAuthor | 단일 책임 명시 (검증), KISS 원칙 | findPostAndUserById (검증 의도 불명확) |
| 호출 시점 | commentRepository.save() 이후 | 댓글이 성공적으로 저장된 후에만 카운트 증가 | save() 이전 (실패 시 롤백 필요) |
| 트랜잭션 범위 | 기존 @Transactional 유지 | 댓글 저장과 카운트 증가를 하나의 트랜잭션으로 묶음 | 별도 트랜잭션 (일관성 문제) |
| 반환 타입 | validatePostAndAuthor는 Pair<Post, User> | 두 엔티티를 동시에 반환하여 가독성 유지 | 별도 호출 유지 (중복) |

## Implementation Todos

### Todo 1: PostRepository에 incrementCommentCount 메서드 추가
- **Priority**: 1
- **Dependencies**: none
- **Goal**: Post의 commentCount를 원자적으로 증가시키는 Repository 메서드 구현
- **Work**:
  - `main-server/src/main/kotlin/com/techtaurant/mainserver/post/infrastructure/out/PostRepository.kt` 파일 열기
  - 인터페이스 본문 끝에 다음 메서드 추가:
    ```kotlin
    /**
     * 게시물의 댓글 수를 원자적으로 1 증가시킵니다.
     * 락을 사용하지 않고 DB 레벨에서 안전하게 처리됩니다.
     *
     * @param postId 댓글 수를 증가시킬 게시물 ID
     */
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    fun incrementCommentCount(@Param("postId") postId: UUID)
    ```
- **Convention Notes**:
  - JPQL 사용 (기존 PostRepository 패턴 준수)
  - JavaDoc 스타일 주석, 한국어 작성
  - 파라미터 설명 포함
- **Verification**:
  - Kotlin 컴파일 에러 없음
  - @Modifying, @Query 어노테이션 올바른 import
- **Exit Criteria**: incrementCommentCount 메서드가 PostRepository에 추가되고 컴파일 성공
- **Status**: pending

### Todo 2: CommentWriteService 리팩토링
- **Priority**: 2
- **Dependencies**: Todo 1
- **Goal**: findPostById와 findUserById를 통합하고, createComment에서 commentCount 증가 로직 추가
- **Work**:
  - `main-server/src/main/kotlin/com/techtaurant/mainserver/comment/application/CommentWriteService.kt` 파일 열기
  - `findPostById`와 `findUserById` 메서드를 삭제하고 다음으로 교체:
    ```kotlin
    /**
     * 게시물과 작성자를 검증합니다.
     *
     * @param postId 게시물 ID
     * @param userId 작성자 ID
     * @return 검증된 Post와 User의 Pair
     * @throws ApiException 게시물 또는 사용자를 찾을 수 없음
     */
    private fun validatePostAndAuthor(postId: UUID, userId: UUID): Pair<Post, User> {
        val post = postRepository.findById(postId).orElseThrow {
            ApiException(PostStatus.POST_NOT_FOUND)
        }
        val user = userRepository.findById(userId).orElseThrow {
            ApiException(UserStatus.ID_NOT_FOUND)
        }
        return Pair(post, user)
    }
    ```
  - `createComment` 메서드 수정:
    - `val author = findUserById(userId)` → 삭제
    - `val post = findPostById(request.postId)` → 삭제
    - 상단에 추가: `val (post, author) = validatePostAndAuthor(request.postId, userId)`
    - `val savedComment = commentRepository.save(comment)` 다음 줄에 추가:
      ```kotlin
      postRepository.incrementCommentCount(post.id!!)
      ```
- **Convention Notes**:
  - 메서드명: `validate*` (검증 의도 명확)
  - Pair destructuring으로 가독성 향상
  - Single Responsibility: 검증 로직과 증가 로직 분리
- **Verification**:
  - Kotlin 컴파일 성공
  - 기존 로직 동작 유지 (검증 순서 동일)
- **Exit Criteria**: CommentWriteService가 리팩토링되고 commentCount 증가 로직이 추가됨
- **Status**: pending

### Todo 3: 통합 테스트 수정
- **Priority**: 2
- **Dependencies**: Todo 2
- **Goal**: CommentWriteService 테스트에 commentCount 증가 검증 추가
- **Work**:
  - `main-server/src/test/kotlin/com/techtaurant/mainserver/comment/application/CommentWriteServiceTest.kt` 파일 찾기
  - 기존 `createComment` 성공 테스트에 검증 추가:
    ```kotlin
    // Then
    assertThat(response).isNotNull()
    assertThat(response.content).isEqualTo(request.content)

    // commentCount 증가 검증 추가
    val updatedPost = postRepository.findById(post.id!!).get()
    assertThat(updatedPost.commentCount).isEqualTo(1)
    ```
  - 추가 테스트 케이스 작성:
    ```kotlin
    @Test
    @DisplayName("댓글 작성 시 게시물의 commentCount가 증가한다")
    fun createCommentIncreasesCommentCount() {
        // Given
        val initialCount = post.commentCount
        val request = CreateCommentRequest(
            postId = post.id!!,
            content = "테스트 댓글",
            parentId = null
        )

        // When
        commentWriteService.createComment(user.id!!, request)

        // Then
        val updatedPost = postRepository.findById(post.id!!).get()
        assertThat(updatedPost.commentCount).isEqualTo(initialCount + 1)
    }
    ```
- **Convention Notes**:
  - Given-When-Then 패턴 준수
  - @DisplayName 한글 작성
  - AssertJ assertThat 사용
- **Verification**:
  - `./gradlew test --tests CommentWriteServiceTest`
  - 모든 테스트 통과
- **Exit Criteria**: 테스트가 추가/수정되고 모든 테스트가 통과함
- **Status**: pending

## Verification Strategy
전체 구현 완료 후 다음을 검증합니다:
1. `./gradlew clean build` - 전체 빌드 성공
2. `./gradlew test` - 모든 테스트 통과
3. 수동 검증:
   - 댓글 작성 API 호출
   - DB에서 posts 테이블의 comment_count 확인
   - 동시에 여러 댓글 작성 시 정확한 카운트 증가 확인

## Progress Tracking
- Total Todos: 3
- Completed: 0
- Status: Planning complete

## Change Log
- 2026-02-06: Plan created
