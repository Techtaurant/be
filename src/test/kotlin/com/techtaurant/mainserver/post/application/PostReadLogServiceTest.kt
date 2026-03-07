package com.techtaurant.mainserver.post.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.entity.PostReadLog
import com.techtaurant.mainserver.post.enums.PostStatus
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostReadLogRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * PostReadLogService 통합 테스트
 *
 * TestContainers를 활용하여 실제 PostgreSQL 데이터베이스 환경에서
 * 게시물 읽음/안읽음 토글 로직을 검증합니다.
 */
@DisplayName("PostReadLogService 통합 테스트")
@Transactional
class PostReadLogServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var postReadLogService: PostReadLogService

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postReadLogRepository: PostReadLogRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var testUser: User
    private lateinit var testPost: Post

    @BeforeEach
    fun setUpTestData() {
        // Given - 테스트 사용자 생성
        testUser =
            userRepository.save(
                User(
                    name = "테스트사용자",
                    email = "test@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "test-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/profile.jpg",
                ),
            )

        // Given - 테스트 카테고리 생성
        val testCategory =
            categoryRepository.save(
                Category(
                    user = testUser,
                    name = "테스트카테고리",
                    path = "테스트카테고리",
                    depth = 1,
                ),
            )

        // Given - 테스트 게시물 생성
        testPost =
            postRepository.save(
                Post(
                    title = "테스트 게시물",
                    content = "테스트 게시물 내용입니다",
                    author = testUser,
                    category = testCategory,
                ),
            )
    }

    @Test
    @DisplayName("읽음 표시하면 읽음 기록이 생성된다")
    fun toggleReadStatus_markAsRead_shouldCreateReadLog() {
        // Given - 읽음 기록이 없는 상태

        // When - 읽음 표시
        postReadLogService.toggleReadStatus(testPost.id!!, testUser.id!!, true)

        // Then - 읽음 기록 생성 확인
        val log = postReadLogRepository.findByPostIdAndUserId(testPost.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.postId).isEqualTo(testPost.id)
    }

    @Test
    @DisplayName("안읽음 표시하면 읽음 기록이 삭제된다")
    fun toggleReadStatus_markAsUnread_shouldDeleteReadLog() {
        // Given - 이미 읽음 표시한 상태
        postReadLogRepository.save(PostReadLog(postId = testPost.id!!, user = testUser))

        // When - 안읽음 표시
        postReadLogService.toggleReadStatus(testPost.id!!, testUser.id!!, false)

        // Then - 읽음 기록 삭제 확인
        val log = postReadLogRepository.findByPostIdAndUserId(testPost.id!!, testUser.id!!)
        assertThat(log).isNull()
    }

    @Test
    @DisplayName("이미 읽음 상태에서 다시 읽음 표시하면 변화가 없다")
    fun toggleReadStatus_duplicateRead_shouldBeIdempotent() {
        // Given - 이미 읽음 표시한 상태
        val existingLog = postReadLogRepository.save(PostReadLog(postId = testPost.id!!, user = testUser))
        val existingLogId = existingLog.id!!

        // When - 다시 읽음 표시
        postReadLogService.toggleReadStatus(testPost.id!!, testUser.id!!, true)

        // Then - 기존 레코드가 유지됨 (새로 생성되지 않음)
        val log = postReadLogRepository.findByPostIdAndUserId(testPost.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.id).isEqualTo(existingLogId)
    }

    @Test
    @DisplayName("이미 안읽음 상태에서 안읽음 표시하면 변화가 없다")
    fun toggleReadStatus_duplicateUnread_shouldBeIdempotent() {
        // Given - 읽음 기록이 없는 상태

        // When - 안읽음 표시
        postReadLogService.toggleReadStatus(testPost.id!!, testUser.id!!, false)

        // Then - 여전히 기록 없음
        val log = postReadLogRepository.findByPostIdAndUserId(testPost.id!!, testUser.id!!)
        assertThat(log).isNull()
    }

    @Test
    @DisplayName("존재하지 않는 게시물에 읽음 표시하면 예외가 발생한다")
    fun toggleReadStatus_withNonExistentPost_shouldThrowException() {
        // Given - 존재하지 않는 게시물 ID
        val nonExistentPostId = UUID.randomUUID()

        // When & Then - ApiException 발생
        assertThatThrownBy {
            postReadLogService.toggleReadStatus(nonExistentPostId, testUser.id!!, true)
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(PostStatus.POST_NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 읽음 표시하면 예외가 발생한다")
    fun toggleReadStatus_withNonExistentUser_shouldThrowException() {
        // Given - 존재하지 않는 사용자 ID
        val nonExistentUserId = UUID.randomUUID()

        // When & Then - ApiException 발생
        assertThatThrownBy {
            postReadLogService.toggleReadStatus(testPost.id!!, nonExistentUserId, true)
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(UserStatus.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("여러 사용자가 동일 게시물에 읽음 표시하면 각각 독립적으로 처리된다")
    fun toggleReadStatus_multipleUsers_shouldHandleIndependently() {
        // Given - 추가 사용자 생성
        val anotherUser =
            userRepository.save(
                User(
                    name = "다른사용자",
                    email = "another@example.com",
                    provider = OAuthProvider.GOOGLE,
                    identifier = "another-id-${UUID.randomUUID()}",
                    role = UserRole.USER,
                    profileImageUrl = "https://example.com/another.jpg",
                ),
            )

        // When - 첫 번째 사용자가 읽음 표시
        postReadLogService.toggleReadStatus(testPost.id!!, testUser.id!!, true)

        // When - 두 번째 사용자는 읽음 표시하지 않음

        // Then - 첫 번째 사용자만 읽음 기록 존재
        val log1 = postReadLogRepository.findByPostIdAndUserId(testPost.id!!, testUser.id!!)
        assertThat(log1).isNotNull

        val log2 = postReadLogRepository.findByPostIdAndUserId(testPost.id!!, anotherUser.id!!)
        assertThat(log2).isNull()
    }
}
