package com.techtaurant.mainserver.link.dto

import com.techtaurant.mainserver.link.entity.Link
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class LinkCursor(
    val publishedAt: Instant?,
    val id: UUID,
) {
    fun encode(): String {
        val raw = "$CURSOR_VERSION$CURSOR_DELIMITER${publishedAt ?: NULL_PUBLISHED_AT}$CURSOR_DELIMITER$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    companion object {
        private const val CURSOR_VERSION = "link-published-v1"
        private const val CURSOR_DELIMITER = "|"
        private const val NULL_PUBLISHED_AT = "null"

        fun decode(cursor: String): LinkCursor? =
            runCatching {
                val decoded = String(Base64.getUrlDecoder().decode(cursor))
                val parts = decoded.split(CURSOR_DELIMITER)
                require(parts.size == 3)
                require(parts[0] == CURSOR_VERSION)
                LinkCursor(parseNullableInstant(parts[1]), UUID.fromString(parts[2]))
            }.getOrNull()

        private fun parseNullableInstant(value: String): Instant? =
            if (value == NULL_PUBLISHED_AT) {
                null
            } else {
                parseInstant(value)
            }

        private fun parseInstant(value: String): Instant = value.toLongOrNull()?.let(Instant::ofEpochMilli) ?: Instant.parse(value)

        fun from(link: Link): LinkCursor = LinkCursor(link.publishedAt, link.id!!)
    }
}
