package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.common.util.HtmlSanitizer
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
        messageKey: String,
        messageArguments: List<String>,
        locale: Locale? = null,
    ): String {
        val messageHtml = buildMessage(messageKey, locale, messageArguments)

        val container = Element("div")
        container.appendElement("span").append(messageHtml)

        return HtmlSanitizer.sanitizeContent(container.outerHtml()).trim()
    }

    fun resolveThumbnailUrl(media: NotificationPayloadMedia): String =
        requireNotNull(sanitizeMediaUrl(media.url)) {
            "알림 썸네일 URL이 안전한 URL 형식이 아닙니다."
        }

    private fun buildMessage(
        key: String,
        locale: Locale?,
        args: List<String>,
    ): String {
        val resolvedLocale = locale ?: LocaleContextHolder.getLocale()
        val sanitizedArgs = args.map(HtmlSanitizer::sanitizeTitle).toTypedArray()
        val localizedMessage = messageSource.getMessage(key, sanitizedArgs, resolvedLocale)

        return HtmlSanitizer.sanitizeContent(localizedMessage).trim()
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
    )
}
