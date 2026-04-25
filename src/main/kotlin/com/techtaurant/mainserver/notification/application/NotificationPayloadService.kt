package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.common.util.HtmlSanitizer
import com.techtaurant.mainserver.notification.enums.NotificationType
import org.jsoup.nodes.Element
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class NotificationPayloadService(
    private val messageSource: MessageSource,
) {
    fun buildPayload(
        type: NotificationType,
        actorName: String,
        postTitle: String? = null,
        media: NotificationPayloadMedia? = null,
        locale: Locale? = null,
    ): String {
        val messageHtml =
            when (type) {
                NotificationType.POST_COMMENT ->
                    buildMessage("notification.payload.post-comment", locale, actorName, postTitle.orEmpty())
                NotificationType.COMMENT_REPLY ->
                    buildMessage("notification.payload.comment-reply", locale, actorName, postTitle.orEmpty())
                NotificationType.FOLLOWER_POST ->
                    buildMessage("notification.payload.follower-post", locale, actorName, postTitle.orEmpty())
                NotificationType.FOLLOW ->
                    buildMessage("notification.payload.follow", locale, actorName)
            }

        val container = Element("div")
        media.toSafeMedia()?.let { safeMedia ->
            container.appendElement("img")
                .attr("src", safeMedia.url)
                .attr("alt", safeMedia.alt)
                .attr("width", "40")
                .attr("height", "40")
        }
        container.appendElement("span").append(messageHtml)

        return HtmlSanitizer.sanitizeContent(container.outerHtml()).trim()
    }

    private fun buildMessage(
        key: String,
        locale: Locale?,
        vararg args: String,
    ): String {
        val resolvedLocale = locale ?: LocaleContextHolder.getLocale()
        val sanitizedArgs = args.map(HtmlSanitizer::sanitizeTitle).toTypedArray()
        val localizedMessage = messageSource.getMessage(key, sanitizedArgs, resolvedLocale)

        return HtmlSanitizer.sanitizeContent(localizedMessage).trim()
    }

    private fun NotificationPayloadMedia?.toSafeMedia(): SafeNotificationPayloadMedia? {
        val payloadMedia = this ?: return null
        val safeUrl = sanitizeMediaUrl(payloadMedia.url) ?: return null
        val safeAlt = HtmlSanitizer.sanitizeTitle(payloadMedia.alt).ifBlank { "notification image" }
        return SafeNotificationPayloadMedia(url = safeUrl, alt = safeAlt)
    }

    private fun sanitizeMediaUrl(url: String?): String? {
        val candidate = url?.trim().orEmpty()
        if (candidate.isBlank()) {
            return null
        }

        return when {
            candidate.startsWith("http://") -> candidate
            candidate.startsWith("https://") -> candidate
            candidate.startsWith("/") -> candidate
            else -> null
        }
    }

    data class NotificationPayloadMedia(
        val url: String,
        val alt: String,
    )

    private data class SafeNotificationPayloadMedia(
        val url: String,
        val alt: String,
    )
}
