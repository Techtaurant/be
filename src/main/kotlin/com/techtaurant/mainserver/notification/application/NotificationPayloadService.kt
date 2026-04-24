package com.techtaurant.mainserver.notification.application

import com.techtaurant.mainserver.common.util.HtmlSanitizer
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class NotificationPayloadService(
    private val messageSource: MessageSource,
) {
    fun buildPostCommentPayload(
        actorName: String,
        postTitle: String,
        locale: Locale? = null,
    ): String = buildMessage("notification.payload.post-comment", locale, actorName, postTitle)

    fun buildCommentReplyPayload(
        actorName: String,
        postTitle: String,
        locale: Locale? = null,
    ): String = buildMessage("notification.payload.comment-reply", locale, actorName, postTitle)

    fun buildFollowerPostPayload(
        actorName: String,
        postTitle: String,
        locale: Locale? = null,
    ): String = buildMessage("notification.payload.follower-post", locale, actorName, postTitle)

    fun buildFollowPayload(
        actorName: String,
        locale: Locale? = null,
    ): String = buildMessage("notification.payload.follow", locale, actorName)

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
}
