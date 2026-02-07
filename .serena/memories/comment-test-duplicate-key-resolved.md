# Comment Integration Test - Duplicate Key Issue Resolved

## 세션 요약
**날짜**: 2026-02-06  
**작업**: CommentControllerIntegrationTest duplicate key constraint 위반 해결  
**상태**: ✅ RESOLVED

## 원래 문제

### 에러 메시지
```
could not execute statement [ERROR: duplicate key value violates unique constraint "users_identifier_provider_key"
  Detail: Key (identifier, provider)=(google-test-id, GOOGLE) already exists.]
```

### 발생 원인
1. `@BeforeEach`에서 매 테스트마다 동일한 `google-test-id` identifier로 사용자 생성
2. Testcontainers DB는 모든 테스트 간 공유되어 데이터 누적
3. RestAssured HTTP 테스트에서 `@Transactional` 롤백 불가

### 사용자 제안
> "Custom User를 생성하고 삭제하는 Singleton 객체를 만들고 이걸 이용해서 구현"

## 해결 방법

### 1. TestUserFactory Singleton 생성

**위치:** `main-server/src/test/kotlin/com/techtaurant/mainserver/base/TestUserFactory.kt`

**핵심 기능:**
```kotlin
object TestUserFactory {
    // 고유한 identifier로 사용자 생성
    fun createTestUser(userRepository: UserRepository): User {
        return userRepository.save(
            User(
                identifier = "test-id-${UUID.randomUUID()}", // ← 고유성 보장
                // ...
            )
        )
    }
    
    // 외래키 순서를 고려한 cleanup
    fun cleanupAllTestData(
        commentRepository, postRepository, 
        categoryRepository, userRepository
    ) {
        commentRepository.deleteAllInBatch()  // 1. 댓글
        postRepository.deleteAllInBatch()      // 2. 게시물
        categoryRepository.deleteAllInBatch()  // 3. 카테고리
        userRepository.deleteAllInBatch()      // 4. 사용자
    }
    
    fun createTestCategory(categoryRepository, user): Category
    fun createTestPost(postRepository, author, category): Post
}
```

### 2. CommentControllerIntegrationTest 리팩토링

**변경 전:**
```kotlin
@BeforeEach
fun setUpTestData() {
    testUser = userRepository.save(
        User(
            identifier = "google-test-id",  // ← 중복 발생
            // ...
        )
    )
    // ...
}
```

**변경 후:**
```kotlin
@BeforeEach
fun setUpTestData() {
    // 1. 명시적 cleanup
    TestUserFactory.cleanupAllTestData(
        commentRepository = commentRepository,
        postRepository = postRepository,
        categoryRepository = categoryRepository,
        userRepository = userRepository
    )
    
    // 2. 고유한 identifier로 사용자 생성
    testUser = TestUserFactory.createTestUser(userRepository)
    
    // 3. 테스트 데이터 생성
    val testCategory = TestUserFactory.createTestCategory(categoryRepository, testUser)
    testPost = TestUserFactory.createTestPost(postRepository, testUser, testCategory)
    
    // 4. JWT 토큰 생성
    accessToken = jwtTokenProvider.createAccessToken(testUser.id!!, testUser.role)
}
```

## 핵심 개념

### RestAssured + @Transactional 문제

**왜 @Transactional이 작동하지 않는가?**
- RestAssured는 실제 HTTP 요청을 전송
- HTTP 요청은 별도의 트랜잭션에서 처리
- 테스트 메서드의 `@Transactional` 롤백이 HTTP 트랜잭션에 영향 없음

**참고:**
- [Quarkus Issue #15436](https://github.com/quarkusio/quarkus/issues/15436)
- [rieckpil.de - Testing Pitfall](https://rieckpil.de/spring-boot-testing-pitfall-transaction-rollback-in-tests/)

### 올바른 패턴

```
❌ @Transactional 롤백 의존
   → RestAssured HTTP 테스트에서 작동 안 함

✅ @BeforeEach cleanup + 고유 identifier
   → 명시적 데이터 정리 + 중복 방지
```

## 테스트 결과

### 변경 전
```
ERROR: duplicate key value violates unique constraint "users_identifier_provider_key"
Detail: Key (identifier, provider)=(google-test-id, GOOGLE) already exists.
```

### 변경 후
```bash
$ ./gradlew test --tests "*CommentControllerIntegrationTest" 2>&1 | grep -i "duplicate\|constraint\|unique"
(출력 없음)  # ← duplicate key 에러 해결!
```

## 추가 작업

### IntegrationTest 개선
- `@ActiveProfiles("test")` 추가
- `application-test.yml` 자동 로드
- JWT secret 등 테스트 설정 적용

### Serena Memory
- `integration-test-guide` 작성
- TestUserFactory 패턴 문서화
- RestAssured + Testcontainers 베스트 프랙티스

## 남은 이슈

### 401 Unauthorized 에러
**상태:** 미해결  
**원인:** JWT 인증 관련 (원래 문제와 무관)  
**영향:** 모든 테스트에서 401 발생  

이것은 별도의 이슈로, 다음 조치 필요:
- JWT 필터 동작 확인
- 테스트 환경 JWT 설정 점검
- SecurityConfig 테스트 환경 설정 검증

## 파일 변경 사항

### 생성
- `main-server/src/test/kotlin/com/techtaurant/mainserver/base/TestUserFactory.kt`

### 수정
- `main-server/src/test/kotlin/com/techtaurant/mainserver/base/IntegrationTest.kt`
  - `@ActiveProfiles("test")` 추가
  
- `main-server/src/test/kotlin/com/techtaurant/mainserver/comment/infrastructure/in/CommentControllerIntegrationTest.kt`
  - TestUserFactory 사용으로 리팩토링
  - 불필요한 import 제거

### Serena Memory
- `integration-test-guide` (신규)
- `comment-test-duplicate-key-resolved` (신규)

## 베스트 프랙티스

### DO ✅
1. TestUserFactory 사용으로 중앙화된 데이터 관리
2. UUID 기반 고유 identifier 자동 생성
3. @BeforeEach에서 명시적 cleanup
4. 외래키 제약조건 순서 고려

### DON'T ❌
1. RestAssured HTTP 테스트에서 @Transactional 의존
2. 하드코딩된 identifier 사용
3. cleanup 순서 무시
4. @ActiveProfiles("test") 누락

## 결론

✅ **원래 문제 (duplicate key constraint) 완전 해결**
- TestUserFactory Singleton 패턴으로 깔끔하게 해결
- 테스트 격리 보장
- 재사용 가능한 구조

❌ **401 Unauthorized는 별도 이슈**
- 원래 문제와 무관
- JWT 인증 관련 문제
- 추가 조사 필요

## 다음 단계

1. ✅ duplicate key 문제 해결 완료
2. ✅ TestUserFactory 패턴 확립
3. ✅ 통합 테스트 가이드 문서화
4. ⏳ 401 JWT 인증 문제 해결 (별도 작업)
5. ⏳ 다른 테스트에 TestUserFactory 패턴 적용
