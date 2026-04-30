package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.config.MessageSourceConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Locale

class NotificationPayloadServiceTest {
    private lateinit var notificationPayloadService: NotificationPayloadService

    @BeforeEach
    fun setUp() {
        notificationPayloadService = NotificationPayloadService(MessageSourceConfig().messageSource())
    }

    @Test
    @DisplayName("댓글 알림 payload는 프로필 이미지와 텍스트를 함께 포함한다")
    fun buildPayload_withActorProfileImage_returnsWrappedHtml() {
        val payload =
            notificationPayloadService.buildPayload(
                messageKey = "notification.payload.post-comment",
                messageArguments = listOf("민수", "JPA 정리"),
                media =
                    NotificationPayloadService.NotificationPayloadMedia(
                        url = "https://cdn.example.com/profile.png",
                        alt = "민수 프로필 이미지",
                    ),
                locale = Locale.KOREAN,
            )

        assertThat(payload).contains("""<img src="https://cdn.example.com/profile.png"""")
        assertThat(payload).contains("""alt="민수 프로필 이미지"""")
        assertThat(payload).contains("""width="40"""")
        assertThat(payload).contains("""height="40"""")
        assertThat(payload).contains("<strong>민수</strong>")
        assertThat(payload).contains("<strong>JPA 정리</strong>")
        assertThat(payload).contains("댓글을 남겼습니다")
    }

    @Test
    @DisplayName("새 글 알림 payload는 게시물 썸네일과 텍스트를 함께 포함한다")
    fun buildPayload_withPostThumbnail_returnsWrappedHtml() {
        val payload =
            notificationPayloadService.buildPayload(
                messageKey = "notification.payload.follower-post",
                messageArguments = listOf("Minsu", "DDD Start"),
                media =
                    NotificationPayloadService.NotificationPayloadMedia(
                        url = "/static/images/post-thumbnail.png",
                        alt = "DDD Start 썸네일",
                    ),
                locale = Locale.ENGLISH,
            )

        assertThat(payload).contains("""<img src="/static/images/post-thumbnail.png"""")
        assertThat(payload).contains("""alt="DDD Start 썸네일"""")
        assertThat(payload).contains("<strong>Minsu</strong>")
        assertThat(payload).contains("<strong>DDD Start</strong>")
        assertThat(payload).contains("published a new post")
    }

    @Test
    @DisplayName("payload 생성 시 위험한 동적 값과 안전하지 않은 media URL은 제거된다")
    fun buildPayload_sanitizesArgumentsAndSkipsUnsafeMediaUrl() {
        val payload =
            notificationPayloadService.buildPayload(
                messageKey = "notification.payload.follow",
                messageArguments = listOf("<script>alert('xss')</script><b>민수</b>"),
                media =
                    NotificationPayloadService.NotificationPayloadMedia(
                        url = "javascript:alert('xss')",
                        alt = "<img src=x onerror=alert('xss')>민수 프로필 이미지",
                    ),
                locale = Locale.KOREAN,
            )

        assertThat(payload).contains("민수")
        assertThat(payload).contains("팔로우했습니다")
        assertThat(payload).doesNotContain("<script>", "<img src=\"javascript:", "onerror", "alert")
    }
}
