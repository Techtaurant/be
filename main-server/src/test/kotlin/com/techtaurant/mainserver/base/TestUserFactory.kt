package com.techtaurant.mainserver.base

import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import java.util.UUID

/**
 * 테스트용 사용자 데이터 생성 및 정리를 담당하는 Singleton 객체
 *
 * ## 사용 목적
 * - 테스트 간 데이터 격리 보장
 * - 중복 사용자 생성 방지
 * - 일관된 테스트 데이터 생성
 *
 * ## 사용 방법
 *
 * ### 패턴 1: @BeforeEach 완전 격리 (모든 데이터 매번 재생성)
 * ```kotlin
 * @BeforeEach
 * fun setUp() {
 *     TestUserFactory.cleanupAllTestData(
 *         commentRepository = commentRepository,
 *         postRepository = postRepository,
 *         categoryRepository = categoryRepository,
 *         userRepository = userRepository
 *     )
 *
 *     testUser = TestUserFactory.createTestUser(userRepository)
 * }
 * ```
 *
 * ### 패턴 2: @BeforeAll 공유 User (User 재사용, 나머지 매번 재생성) - 권장
 * ```kotlin
 * companion object {
 *     lateinit var sharedTestUser: User
 *
 *     @BeforeAll
 *     @JvmStatic
 *     fun setUpSharedUser(@Autowired userRepository: UserRepository) {
 *         sharedTestUser = TestUserFactory.createTestUser(userRepository)
 *     }
 *
 *     @AfterAll
 *     @JvmStatic
 *     fun tearDownSharedUser(
 *         @Autowired commentRepository: CommentRepository,
 *         @Autowired postRepository: PostRepository,
 *         @Autowired categoryRepository: CategoryRepository,
 *         @Autowired userRepository: UserRepository
 *     ) {
 *         TestUserFactory.cleanupAllTestData(
 *             commentRepository, postRepository, categoryRepository, userRepository
 *         )
 *     }
 * }
 *
 * @BeforeEach
 * fun setUpTestData() {
 *     // User는 유지, 나머지만 cleanup
 *     TestUserFactory.cleanupDependentData(
 *         commentRepository, postRepository, categoryRepository
 *     )
 *
 *     testCategory = TestUserFactory.createTestCategory(categoryRepository, sharedTestUser)
 *     testPost = TestUserFactory.createTestPost(postRepository, sharedTestUser, testCategory)
 *     accessToken = jwtTokenProvider.createAccessToken(sharedTestUser.id!!, sharedTestUser.role)
 * }
 * ```
 *
 * ## 주의사항
 * - RestAssured HTTP 테스트에서는 @Transactional 롤백이 작동하지 않음
 * - 따라서 각 테스트 전 명시적 cleanup 필요
 * - 외래키 제약조건 순서를 고려하여 cleanup 수행
 * - 패턴 2 사용 시 User 데이터를 변경하는 테스트는 별도 격리 필요
 */
object TestUserFactory {
    /**
     * 고유한 identifier를 가진 테스트 사용자를 생성합니다.
     *
     * @param userRepository 사용자 저장소
     * @param name 사용자 이름 (기본값: "테스트사용자")
     * @param email 이메일 (기본값: "test@example.com")
     * @param provider OAuth 제공자 (기본값: GOOGLE)
     * @param role 사용자 권한 (기본값: USER)
     * @return 생성된 테스트 사용자
     */
    fun createTestUser(
        userRepository: UserRepository,
        name: String = "테스트사용자",
        email: String = "test@example.com",
        provider: OAuthProvider = OAuthProvider.GOOGLE,
        role: UserRole = UserRole.USER,
    ): User {
        return userRepository.save(
            User(
                name = name,
                email = email,
                provider = provider,
                identifier = "test-id-${UUID.randomUUID()}", // 고유한 identifier로 중복 방지
                role = role,
                profileImageUrl = "https://example.com/profile.jpg",
            ),
        )
    }

    /**
     * 테스트용 카테고리를 생성합니다.
     *
     * @param categoryRepository 카테고리 저장소
     * @param user 카테고리 소유자
     * @param name 카테고리 이름 (기본값: "테스트카테고리")
     * @return 생성된 테스트 카테고리
     */
    fun createTestCategory(
        categoryRepository: CategoryRepository,
        user: User,
        name: String = "테스트카테고리",
    ): Category {
        return categoryRepository.save(
            Category(
                user = user,
                name = name,
                path = name,
                depth = 1,
            ),
        )
    }

    /**
     * 테스트용 게시물을 생성합니다.
     *
     * @param postRepository 게시물 저장소
     * @param author 작성자
     * @param category 카테고리 (nullable)
     * @param title 게시물 제목 (기본값: "테스트 게시물")
     * @param content 게시물 내용 (기본값: "테스트 게시물 내용입니다")
     * @return 생성된 테스트 게시물
     */
    fun createTestPost(
        postRepository: PostRepository,
        author: User,
        category: Category? = null,
        title: String = "테스트 게시물",
        content: String = "테스트 게시물 내용입니다",
    ): Post {
        return postRepository.save(
            Post(
                title = title,
                content = content,
                author = author,
                category = category,
            ),
        )
    }

    /**
     * User를 제외한 종속 데이터만 정리합니다.
     *
     * @BeforeAll로 생성한 공유 User를 유지하면서
     * 각 테스트에서 생성된 댓글/게시물/카테고리만 정리할 때 사용합니다.
     *
     * ## 사용 예시
     * ```kotlin
     * companion object {
     *     lateinit var sharedTestUser: User
     *
     *     @BeforeAll
     *     @JvmStatic
     *     fun setUpSharedUser(@Autowired userRepository: UserRepository) {
     *         sharedTestUser = TestUserFactory.createTestUser(userRepository)
     *     }
     * }
     *
     * @BeforeEach
     * fun setUpTestData() {
     *     // User는 유지, 나머지만 cleanup
     *     TestUserFactory.cleanupDependentData(
     *         commentRepository, postRepository, categoryRepository
     *     )
     *
     *     testCategory = TestUserFactory.createTestCategory(categoryRepository, sharedTestUser)
     *     testPost = TestUserFactory.createTestPost(postRepository, sharedTestUser, testCategory)
     * }
     * ```
     *
     * ## 외래키 제약조건 순서
     * 1. 댓글 (comments) - post_id 참조
     * 2. 게시물 (posts) - author_id, category_id 참조
     * 3. 카테고리 (categories) - user_id 참조
     *
     * @param commentRepository 댓글 저장소
     * @param postRepository 게시물 저장소
     * @param categoryRepository 카테고리 저장소
     */
    fun cleanupDependentData(
        commentRepository: Any,
        postRepository: Any,
        categoryRepository: Any,
    ) {
        // 외래키 제약조건 순서에 따라 역순으로 삭제 (User는 제외)
        (commentRepository as? org.springframework.data.jpa.repository.JpaRepository<*, *>)?.deleteAllInBatch()
        (postRepository as? org.springframework.data.jpa.repository.JpaRepository<*, *>)?.deleteAllInBatch()
        (categoryRepository as? org.springframework.data.jpa.repository.JpaRepository<*, *>)?.deleteAllInBatch()
    }

    /**
     * 모든 테스트 데이터를 정리합니다.
     *
     * RestAssured HTTP 테스트에서는 별도 트랜잭션으로 동작하여
     * @Transactional 롤백이 적용되지 않으므로 명시적 cleanup이 필요합니다.
     *
     * ## 외래키 제약조건 순서
     * 1. 댓글 (comments) - post_id 참조
     * 2. 게시물 (posts) - author_id, category_id 참조
     * 3. 카테고리 (categories) - user_id 참조
     * 4. 사용자 (users) - 참조 없음
     *
     * @param commentRepository 댓글 저장소
     * @param postRepository 게시물 저장소
     * @param categoryRepository 카테고리 저장소
     * @param userRepository 사용자 저장소
     */
    fun cleanupAllTestData(
        commentRepository: Any,
        postRepository: Any,
        categoryRepository: Any,
        userRepository: Any,
    ) {
        // 외래키 제약조건 순서에 따라 역순으로 삭제
        (commentRepository as? org.springframework.data.jpa.repository.JpaRepository<*, *>)?.deleteAllInBatch()
        (postRepository as? org.springframework.data.jpa.repository.JpaRepository<*, *>)?.deleteAllInBatch()
        (categoryRepository as? org.springframework.data.jpa.repository.JpaRepository<*, *>)?.deleteAllInBatch()
        (userRepository as? org.springframework.data.jpa.repository.JpaRepository<*, *>)?.deleteAllInBatch()
    }
}
