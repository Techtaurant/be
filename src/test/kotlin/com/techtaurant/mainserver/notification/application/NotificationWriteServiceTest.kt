package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.base.IntegrationTest
import com.techtaurant.mainserver.comment.entity.Comment
import com.techtaurant.mainserver.comment.infrastructure.out.CommentRepository
import com.techtaurant.mainserver.notification.enums.NotificationTargetRole
import com.techtaurant.mainserver.notification.enums.NotificationTargetType
import com.techtaurant.mainserver.notification.enums.NotificationType
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationRecipientRepository
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationRepository
import com.techtaurant.mainserver.notification.infrastructure.out.NotificationTargetRepository
import com.techtaurant.mainserver.post.entity.Post
import com.techtaurant.mainserver.post.infrastructure.out.PostRepository
import com.techtaurant.mainserver.security.enums.OAuthProvider
import com.techtaurant.mainserver.user.entity.User
import com.techtaurant.mainserver.user.enums.UserRole
import com.techtaurant.mainserver.user.infrastructure.out.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.UUID

@Transactional
@ActiveProfiles("test")
class NotificationWriteServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var notificationWriteService: NotificationWriteService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationTargetRepository: NotificationTargetRepository

    @Autowired
    private lateinit var notificationRecipientRepository: NotificationRecipientRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var commentRepository: CommentRepository

    private lateinit var actorUser: User
    private lateinit var recipientUser: User
    private lateinit var secondRecipientUser: User
    private lateinit var post: Post
    private lateinit var comment: Comment

    @BeforeEach
    fun setUpTestData() {
        actorUser = createUser("actor")
        recipientUser = createUser("recipient")
        secondRecipientUser = createUser("recipient-second")
        post = createPost(actorUser)
        comment = createComment(post = post, author = recipientUser)
    }

    @Test
    @DisplayName("게시물 댓글 알림 생성 시 payload HTML은 sanitize되고 target과 recipient가 저장된다")
    fun createPostCommentNotification_savesNotificationGraph() {
        val notificationId =
            notificationWriteService.createPostCommentNotification(
                actorUserId = actorUser.id!!,
                recipientUserId = recipientUser.id!!,
                postId = post.id!!,
                commentId = comment.id!!,
                locale = Locale.KOREAN,
            )

        val savedNotification = notificationRepository.findById(notificationId).orElseThrow()
        val savedTargets = notificationTargetRepository.findAllByNotificationIdOrderByCreatedAtAsc(notificationId)
        val savedRecipients = notificationRecipientRepository.findAllByNotificationIdOrderByCreatedAtAsc(notificationId)

        assertThat(savedNotification.type).isEqualTo(NotificationType.POST_COMMENT)
        assertThat(savedNotification.payloadHtml).contains("<strong>${actorUser.name}</strong>")
        assertThat(savedNotification.payloadHtml).contains("<strong>${post.title}</strong>")
        assertThat(savedNotification.payloadHtml).contains("댓글을 남겼습니다")
        assertThat(savedTargets)
            .extracting("role", "targetType", "targetId")
            .containsExactlyInAnyOrder(
                tuple(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actorUser.id),
                tuple(NotificationTargetRole.TARGET, NotificationTargetType.POST, post.id),
                tuple(NotificationTargetRole.TARGET, NotificationTargetType.COMMENT, comment.id),
            )
        assertThat(savedRecipients).singleElement().extracting("user.id").isEqualTo(recipientUser.id)
    }

    @Test
    @DisplayName("대댓글 알림 생성 시 COMMENT_REPLY 타입으로 저장된다")
    fun createCommentReplyNotification_savesReplyNotification() {
        val replyComment = createComment(post = post, author = actorUser, parent = comment)

        val notificationId =
            notificationWriteService.createCommentReplyNotification(
                actorUserId = actorUser.id!!,
                recipientUserId = recipientUser.id!!,
                postId = post.id!!,
                commentId = replyComment.id!!,
                locale = Locale.ENGLISH,
            )

        val savedNotification = notificationRepository.findById(notificationId).orElseThrow()
        val savedTargets = notificationTargetRepository.findAllByNotificationIdOrderByCreatedAtAsc(notificationId)

        assertThat(savedNotification.type).isEqualTo(NotificationType.COMMENT_REPLY)
        assertThat(savedNotification.payloadHtml).contains("<strong>${actorUser.name}</strong>")
        assertThat(savedNotification.payloadHtml).contains("replied to your comment")
        assertThat(savedTargets)
            .extracting("role", "targetType", "targetId")
            .containsExactlyInAnyOrder(
                tuple(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actorUser.id),
                tuple(NotificationTargetRole.TARGET, NotificationTargetType.POST, post.id),
                tuple(NotificationTargetRole.TARGET, NotificationTargetType.COMMENT, replyComment.id),
            )
    }

    @Test
    @DisplayName("팔로잉 게시물 알림 생성 시 recipient는 중복 없이 저장된다")
    fun createFollowerPostNotification_deduplicatesRecipients() {
        val notificationId =
            notificationWriteService.createFollowerPostNotification(
                actorUserId = actorUser.id!!,
                recipientUserIds = listOf(recipientUser.id!!, secondRecipientUser.id!!, recipientUser.id!!),
                postId = post.id!!,
                locale = Locale.ENGLISH,
            )

        val savedNotification = notificationRepository.findById(notificationId).orElseThrow()
        val savedTargets = notificationTargetRepository.findAllByNotificationIdOrderByCreatedAtAsc(notificationId)
        val savedRecipients = notificationRecipientRepository.findAllByNotificationIdOrderByCreatedAtAsc(notificationId)

        assertThat(savedNotification.type).isEqualTo(NotificationType.FOLLOWER_POST)
        assertThat(savedNotification.payloadHtml).contains("published a new post")
        assertThat(savedNotification.payloadHtml).contains("<strong>${post.title}</strong>")
        assertThat(savedTargets)
            .extracting("role", "targetType", "targetId")
            .containsExactlyInAnyOrder(
                tuple(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actorUser.id),
                tuple(NotificationTargetRole.TARGET, NotificationTargetType.POST, post.id),
            )
        assertThat(savedRecipients).hasSize(2)
        assertThat(savedRecipients.map { it.user.id }).containsExactly(recipientUser.id, secondRecipientUser.id)
    }

    @Test
    @DisplayName("팔로우 알림 생성 시 actor와 팔로우 당한 target user가 함께 저장된다")
    fun createFollowNotification_savesUserTargets() {
        val notificationId =
            notificationWriteService.createFollowNotification(
                actorUserId = actorUser.id!!,
                recipientUserId = recipientUser.id!!,
                locale = Locale.KOREAN,
            )

        val savedNotification = notificationRepository.findById(notificationId).orElseThrow()
        val savedTargets = notificationTargetRepository.findAllByNotificationIdOrderByCreatedAtAsc(notificationId)
        val savedRecipients = notificationRecipientRepository.findAllByNotificationIdOrderByCreatedAtAsc(notificationId)

        assertThat(savedNotification.type).isEqualTo(NotificationType.FOLLOW)
        assertThat(savedNotification.payloadHtml).contains("<strong>${actorUser.name}</strong>")
        assertThat(savedNotification.payloadHtml).contains("팔로우했습니다")
        assertThat(savedTargets)
            .extracting("role", "targetType", "targetId")
            .containsExactlyInAnyOrder(
                tuple(NotificationTargetRole.ACTOR, NotificationTargetType.USER, actorUser.id),
                tuple(NotificationTargetRole.TARGET, NotificationTargetType.USER, recipientUser.id),
            )
        assertThat(savedRecipients).singleElement().extracting("user.id").isEqualTo(recipientUser.id)
    }

    private fun createUser(prefix: String): User {
        val uniqueSuffix = UUID.randomUUID().toString().take(8)
        return userRepository.save(
            User(
                name = "$prefix-$uniqueSuffix",
                email = "$prefix-$uniqueSuffix@example.com",
                provider = OAuthProvider.GOOGLE,
                identifier = "$prefix-id-$uniqueSuffix",
                role = UserRole.USER,
                profileImageUrl = "https://example.com/$prefix-$uniqueSuffix.jpg",
            ),
        )
    }

    private fun createPost(author: User): Post {
        return postRepository.save(
            Post(
                title = "알림 테스트 게시물",
                content = "<p>본문</p>",
                author = author,
            ),
        )
    }

    private fun createComment(
        post: Post,
        author: User,
        parent: Comment? = null,
    ): Comment {
        return commentRepository.save(
            Comment(
                content = "<p>댓글</p>",
                post = post,
                author = author,
                parent = parent,
                depth = if (parent == null) 0 else 1,
            ),
        )
    }

    private fun tuple(
        role: NotificationTargetRole,
        targetType: NotificationTargetType,
        targetId: UUID?,
    ): org.assertj.core.groups.Tuple =
        org.assertj.core.groups.Tuple.tuple(role, targetType, targetId)
}
