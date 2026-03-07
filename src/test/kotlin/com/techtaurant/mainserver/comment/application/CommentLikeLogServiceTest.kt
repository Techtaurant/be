package com.techtaurant.mainserver.comment.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.enums.CommentStatus
import com.techtaurant.mainserver.comment.infrastructure.out.CommentLikeLogRepository
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.common.enums.LikeStatus
import com.techtaurant.mainserver.common.exception.ApiException
import com.techtaurant.mainserver.post.entity.Category
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.CategoryRepository
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.enums.UserStatus
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * CommentLikeLogService 통합 테스트
 *
 * TestContainers를 활용하여 실제 PostgreSQL 데이터베이스 환경에서
 * 댓글 좋아요/싫어요 로직을 검증합니다.
 */
@DisplayName("CommentLikeLogService 통합 테스트")
@Transactional
class CommentLikeLogServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var commentLikeLogService: CommentLikeLogService

    @Autowired
    private lateinit var commentRepository: CommentRepository

    @Autowired
    private lateinit var commentLikeLogRepository: CommentLikeLogRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: User
    private lateinit var testPost: Post
    private lateinit var testComment: Comment

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

        // Given - 테스트 댓글 생성
        testComment =
            commentRepository.save(
                Comment(
                    content = "테스트 댓글입니다",
                    post = testPost,
                    author = testUser,
                    parent = null,
                    depth = 0,
                ),
            )
    }

    @Test
    @DisplayName("중립 상태에서 좋아요를 기록하면 likeCount가 1 증가한다")
    fun recordLike_fromNeutralToLike_shouldIncrementLikeCount() {
        // Given - 초기 likeCount = 0

        // When - 좋아요 기록
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.LIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(testComment)

        // Then - likeCount가 1 증가
        assertThat(testComment.likeCount).isEqualTo(1)

        // Then - 좋아요 로그 생성 확인
        val log = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.isLiked).isTrue()
    }

    @Test
    @DisplayName("중립 상태에서 싫어요를 기록하면 likeCount가 1 감소한다")
    fun recordLike_fromNeutralToDislike_shouldDecrementLikeCount() {
        // Given - 초기 likeCount = 0

        // When - 싫어요 기록
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.DISLIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(testComment)

        // Then - likeCount가 1 감소
        assertThat(testComment.likeCount).isEqualTo(-1)

        // Then - 싫어요 로그 생성 확인
        val log = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.isLiked).isFalse()
    }

    @Test
    @DisplayName("좋아요 상태에서 싫어요로 변경하면 likeCount가 2 감소한다")
    fun recordLike_fromLikeToDislike_shouldDecrementLikeCountByTwo() {
        // Given - 이미 좋아요한 상태 (likeCount = 1)
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.LIKE)
        entityManager.flush()
        entityManager.refresh(testComment)
        val initialLikeCount = testComment.likeCount

        // When - 싫어요로 변경
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.DISLIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(testComment)

        // Then - likeCount가 2 감소 (좋아요 취소 -1 + 싫어요 적용 -1)
        assertThat(testComment.likeCount).isEqualTo(initialLikeCount - 2)

        // Then - 싫어요 로그로 업데이트 확인
        val log = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.isLiked).isFalse()
    }

    @Test
    @DisplayName("싫어요 상태에서 좋아요로 변경하면 likeCount가 2 증가한다")
    fun recordLike_fromDislikeToLike_shouldIncrementLikeCountByTwo() {
        // Given - 이미 싫어요한 상태 (likeCount = -1)
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.DISLIKE)
        entityManager.flush()
        entityManager.refresh(testComment)
        val initialLikeCount = testComment.likeCount

        // When - 좋아요로 변경
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.LIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(testComment)

        // Then - likeCount가 2 증가 (싫어요 취소 +1 + 좋아요 적용 +1)
        assertThat(testComment.likeCount).isEqualTo(initialLikeCount + 2)

        // Then - 좋아요 로그로 업데이트 확인
        val log = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.isLiked).isTrue()
    }

    @Test
    @DisplayName("이미 좋아요한 상태에서 다시 좋아요를 기록하면 likeCount 변화가 없다")
    fun recordLike_duplicateLike_shouldNotChangeLikeCount() {
        // Given - 이미 좋아요한 상태
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.LIKE)
        entityManager.flush()
        entityManager.refresh(testComment)
        val initialLikeCount = testComment.likeCount

        // When - 다시 좋아요 기록
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.LIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(testComment)

        // Then - likeCount 변화 없음
        assertThat(testComment.likeCount).isEqualTo(initialLikeCount)

        // Then - 로그는 여전히 좋아요 상태
        val log = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.isLiked).isTrue()
    }

    @Test
    @DisplayName("이미 싫어요한 상태에서 다시 싫어요를 기록하면 likeCount 변화가 없다")
    fun recordLike_duplicateDislike_shouldNotChangeLikeCount() {
        // Given - 이미 싫어요한 상태
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.DISLIKE)
        entityManager.flush()
        entityManager.refresh(testComment)
        val initialLikeCount = testComment.likeCount

        // When - 다시 싫어요 기록
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.DISLIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(testComment)

        // Then - likeCount 변화 없음
        assertThat(testComment.likeCount).isEqualTo(initialLikeCount)

        // Then - 로그는 여전히 싫어요 상태
        val log = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, testUser.id!!)
        assertThat(log).isNotNull
        assertThat(log?.isLiked).isFalse()
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요를 기록하면 예외가 발생한다")
    fun recordLike_withNonExistentComment_shouldThrowException() {
        // Given - 존재하지 않는 댓글 ID
        val nonExistentCommentId = UUID.randomUUID()

        // When & Then - ApiException 발생
        assertThatThrownBy {
            commentLikeLogService.recordLike(nonExistentCommentId, testUser.id!!, LikeStatus.LIKE)
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(CommentStatus.COMMENT_NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 좋아요를 기록하면 예외가 발생한다")
    fun recordLike_withNonExistentUser_shouldThrowException() {
        // Given - 존재하지 않는 사용자 ID
        val nonExistentUserId = UUID.randomUUID()

        // When & Then - ApiException 발생
        assertThatThrownBy {
            commentLikeLogService.recordLike(testComment.id!!, nonExistentUserId, LikeStatus.LIKE)
        }
            .isInstanceOf(ApiException::class.java)
            .extracting { (it as ApiException).status }
            .isEqualTo(UserStatus.ID_NOT_FOUND)
    }

    @Test
    @DisplayName("여러 사용자가 동일 댓글에 좋아요를 기록하면 각각 독립적으로 처리된다")
    fun recordLike_multipleUsers_shouldHandleIndependently() {
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

        // When - 첫 번째 사용자가 좋아요
        commentLikeLogService.recordLike(testComment.id!!, testUser.id!!, LikeStatus.LIKE)

        // When - 두 번째 사용자가 싫어요
        commentLikeLogService.recordLike(testComment.id!!, anotherUser.id!!, LikeStatus.DISLIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(testComment)

        // Then - likeCount는 0 (좋아요 +1, 싫어요 -1)
        assertThat(testComment.likeCount).isEqualTo(0)

        // Then - 각 사용자의 로그가 독립적으로 존재
        val log1 = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, testUser.id!!)
        assertThat(log1?.isLiked).isTrue()

        val log2 = commentLikeLogRepository.findByCommentIdAndUserId(testComment.id!!, anotherUser.id!!)
        assertThat(log2?.isLiked).isFalse()
    }

    @Test
    @DisplayName("대댓글에도 좋아요/싫어요를 기록할 수 있다")
    fun recordLike_onReply_shouldWork() {
        // Given - 대댓글 생성
        val reply =
            commentRepository.save(
                Comment(
                    content = "대댓글입니다",
                    post = testPost,
                    author = testUser,
                    parent = testComment,
                    depth = 1,
                ),
            )

        // When - 대댓글에 좋아요
        commentLikeLogService.recordLike(reply.id!!, testUser.id!!, LikeStatus.LIKE)

        // Then - 변경사항 DB 반영 및 1차 캐시 갱신
        entityManager.flush()
        entityManager.refresh(reply)
        entityManager.refresh(testComment)

        // Then - 대댓글의 likeCount 증가
        assertThat(reply.likeCount).isEqualTo(1)

        // Then - 부모 댓글의 likeCount는 영향받지 않음
        assertThat(testComment.likeCount).isEqualTo(0)
    }
}
