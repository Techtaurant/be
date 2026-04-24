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
    @DisplayName("게시물 댓글 알림 payload는 한국어 템플릿으로 생성된다")
    fun buildPostCommentPayload_returnsKoreanMessage() {
        val payload =
            notificationPayloadService.buildPostCommentPayload(
                actorName = "민수",
                postTitle = "JPA 정리",
                locale = Locale.KOREAN,
            )

        assertThat(payload).isEqualTo("<strong>민수</strong>님이 회원님의 게시물 <strong>JPA 정리</strong>에 댓글을 남겼습니다.")
    }

    @Test
    @DisplayName("팔로우 알림 payload는 영어 템플릿으로 생성된다")
    fun buildFollowPayload_returnsEnglishMessage() {
        val payload =
            notificationPayloadService.buildFollowPayload(
                actorName = "Minsu",
                locale = Locale.ENGLISH,
            )

        assertThat(payload).isEqualTo("<strong>Minsu</strong> started following you.")
    }

    @Test
    @DisplayName("payload 생성 시 동적 값의 위험한 HTML은 제거된다")
    fun buildFollowerPostPayload_sanitizesArguments() {
        val payload =
            notificationPayloadService.buildFollowerPostPayload(
                actorName = "<script>alert('xss')</script><b>민수</b>",
                postTitle = "<img src=x onerror=alert('xss')>알림 설계",
                locale = Locale.KOREAN,
            )

        assertThat(payload).contains("민수")
        assertThat(payload).contains("알림 설계")
        assertThat(payload).doesNotContain("<script>", "<img", "onerror", "alert")
    }
}
