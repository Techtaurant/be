package com.techtaurant.mainserver.link.dto

import com.techtaurant.mainserver.link.entity.Link
import java.time.Instant
import java.util.UUID

data class LinkCursor(
    val publishedAt: Instant?,
    val id: UUID,
) {
    fun encode(): String = "${publishedAt ?: NULL_PUBLISHED_AT}$CURSOR_DELIMITER$id"

    companion object {
        private const val CURSOR_DELIMITER = "_"
        private const val NULL_PUBLISHED_AT = "null"

        fun decode(cursor: String): LinkCursor? =
            runCatching {
                val parts = cursor.split(CURSOR_DELIMITER, limit = 2)
                require(parts.size == 2)
                LinkCursor(parseNullableInstant(parts[0]), UUID.fromString(parts[1]))
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
